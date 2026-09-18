#include "ps1_retro_bridge.h"
#include <dlfcn.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <cstring>
#include <cstdarg>
#include <string>
#include <vector>
#include <mutex>
#include <sys/stat.h>

#define LOG_TAG "PS1NativeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static constexpr unsigned RETRO_ENVIRONMENT_SET_ROTATION = 1;
static constexpr unsigned RETRO_ENVIRONMENT_GET_OVERSCAN = 2;
static constexpr unsigned RETRO_ENVIRONMENT_GET_CAN_DUPE = 3;
static constexpr unsigned RETRO_ENVIRONMENT_SET_MESSAGE = 6;
static constexpr unsigned RETRO_ENVIRONMENT_SHUTDOWN = 7;
static constexpr unsigned RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL = 8;
static constexpr unsigned RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
static constexpr unsigned RETRO_ENVIRONMENT_SET_PIXEL_FORMAT = 10;
static constexpr unsigned RETRO_ENVIRONMENT_GET_VARIABLE = 15;
static constexpr unsigned RETRO_ENVIRONMENT_SET_VARIABLES = 16;
static constexpr unsigned RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE = 17;
static constexpr unsigned RETRO_ENVIRONMENT_GET_LOG_INTERFACE = 27;
static constexpr unsigned RETRO_ENVIRONMENT_GET_PERF_INTERFACE = 28;
static constexpr unsigned RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY = 31;
static constexpr unsigned RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO = 32;
static constexpr unsigned RETRO_ENVIRONMENT_SET_GEOMETRY = 37;
static constexpr unsigned RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE = 47;
static constexpr unsigned RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE_EXT = 60;

struct retro_variable {
    const char *key;
    const char *value;
};

enum retro_log_level {
    RETRO_LOG_DEBUG = 0,
    RETRO_LOG_INFO,
    RETRO_LOG_WARN,
    RETRO_LOG_ERROR,
    RETRO_LOG_DUMMY = 0x7fffffff
};

typedef void (*retro_log_printf_t)(enum retro_log_level level, const char *fmt, ...);

struct retro_log_callback {
    retro_log_printf_t log;
};

static void *g_core_dl_handle = nullptr;
static ANativeWindow *g_native_window = nullptr;
static std::mutex g_window_mutex;
static unsigned g_last_window_w = 0;
static unsigned g_last_window_h = 0;
static bool g_core_initialized = false;
static bool g_game_loaded = false;
static std::string g_system_directory;
static std::string g_save_directory;
static std::string g_last_error;

static retro_init_t g_retro_init = nullptr;
static retro_deinit_t g_retro_deinit = nullptr;
static retro_load_game_t g_retro_load_game = nullptr;
static retro_unload_game_t g_retro_unload_game = nullptr;
static retro_run_t g_retro_run = nullptr;
static retro_serialize_size_t g_retro_serialize_size = nullptr;
static retro_serialize_t g_retro_serialize = nullptr;
static retro_unserialize_t g_retro_unserialize = nullptr;
static enum retro_pixel_format g_pixel_format = RETRO_PIXEL_FORMAT_RGB565;
static uint16_t g_p1_input_mask = 0, g_p2_input_mask = 0;

static void set_error(const std::string &message) {
    g_last_error = message;
    LOGE("%s", g_last_error.c_str());
}

static void retro_log_cb(enum retro_log_level level, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    int android_level = ANDROID_LOG_INFO;
    switch (level) {
        case RETRO_LOG_DEBUG: android_level = ANDROID_LOG_DEBUG; break;
        case RETRO_LOG_INFO:  android_level = ANDROID_LOG_INFO;  break;
        case RETRO_LOG_WARN:  android_level = ANDROID_LOG_WARN;  break;
        case RETRO_LOG_ERROR: android_level = ANDROID_LOG_ERROR; break;
        default: android_level = ANDROID_LOG_INFO; break;
    }
    __android_log_vprint(android_level, LOG_TAG, fmt, args);
    va_end(args);
}

/*
 * Ultra-fast native frame renderer.
 * Sets the native window buffer geometry directly to the Libretro framebuffer dimensions,
 * allowing Android's hardware GPU (SurfaceFlinger) to handle scaling smoothly at 60 FPS
 * with near-zero CPU overhead.
 */
static void retro_video_refresh_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data || !width || !height) return;

    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (!g_native_window) return;

    if (g_last_window_w != width || g_last_window_h != height) {
        int32_t res = ANativeWindow_setBuffersGeometry(g_native_window, static_cast<int32_t>(width), static_cast<int32_t>(height), WINDOW_FORMAT_RGBA_8888);
        if (res == 0) {
            g_last_window_w = width;
            g_last_window_h = height;
        }
    }

    ANativeWindow_Buffer b;
    if (ANativeWindow_lock(g_native_window, &b, nullptr) != 0) {
        g_last_window_w = 0;
        g_last_window_h = 0;
        return;
    }
    if (!b.bits || b.width <= 0 || b.height <= 0 || b.stride <= 0) {
        ANativeWindow_unlockAndPost(g_native_window);
        return;
    }

    const unsigned renderW = std::min(width, static_cast<unsigned>(b.width));
    const unsigned renderH = std::min(height, static_cast<unsigned>(b.height));
    const size_t dstStride = static_cast<size_t>(b.stride);

    if (b.format == WINDOW_FORMAT_RGB_565) {
        auto *dst = static_cast<uint16_t *>(b.bits);
        const auto *src = static_cast<const uint8_t *>(data);

        if (g_pixel_format == RETRO_PIXEL_FORMAT_RGB565 || g_pixel_format == RETRO_PIXEL_FORMAT_0RGB1555) {
            for (unsigned y = 0; y < renderH; ++y) {
                const auto *srcRow = reinterpret_cast<const uint16_t *>(src + y * pitch);
                auto *dstRow = dst + y * dstStride;
                memcpy(dstRow, srcRow, renderW * sizeof(uint16_t));
            }
        } else if (g_pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
            for (unsigned y = 0; y < renderH; ++y) {
                const auto *srcRow = reinterpret_cast<const uint32_t *>(src + y * pitch);
                auto *dstRow = dst + y * dstStride;
                for (unsigned x = 0; x < renderW; ++x) {
                    const uint32_t c = srcRow[x];
                    const uint16_t r = (c >> 19) & 0x1F;
                    const uint16_t g = (c >> 10) & 0x3F;
                    const uint16_t b_val = (c >> 3) & 0x1F;
                    dstRow[x] = static_cast<uint16_t>((r << 11) | (g << 5) | b_val);
                }
            }
        }
    } else {
        // Standard 32-bit RGBA_8888 buffer
        auto *dst = static_cast<uint32_t *>(b.bits);
        const auto *src = static_cast<const uint8_t *>(data);

        if (g_pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
            for (unsigned y = 0; y < renderH; ++y) {
                const auto *srcRow = reinterpret_cast<const uint16_t *>(src + y * pitch);
                auto *dstRow = dst + y * dstStride;
                for (unsigned x = 0; x < renderW; ++x) {
                    const uint16_t c = srcRow[x];
                    uint32_t r = (c >> 11) & 0x1F;
                    uint32_t g = (c >> 5) & 0x3F;
                    uint32_t b_val = c & 0x1F;
                    r = (r << 3) | (r >> 2);
                    g = (g << 2) | (g >> 4);
                    b_val = (b_val << 3) | (b_val >> 2);
                    dstRow[x] = 0xFF000000 | (b_val << 16) | (g << 8) | r;
                }
            }
        } else if (g_pixel_format == RETRO_PIXEL_FORMAT_0RGB1555) {
            for (unsigned y = 0; y < renderH; ++y) {
                const auto *srcRow = reinterpret_cast<const uint16_t *>(src + y * pitch);
                auto *dstRow = dst + y * dstStride;
                for (unsigned x = 0; x < renderW; ++x) {
                    const uint16_t c = srcRow[x];
                    uint32_t r = (c >> 10) & 0x1F;
                    uint32_t g = (c >> 5) & 0x1F;
                    uint32_t b_val = c & 0x1F;
                    r = (r << 3) | (r >> 2);
                    g = (g << 3) | (g >> 2);
                    b_val = (b_val << 3) | (b_val >> 2);
                    dstRow[x] = 0xFF000000 | (b_val << 16) | (g << 8) | r;
                }
            }
        } else if (g_pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
            for (unsigned y = 0; y < renderH; ++y) {
                const auto *srcRow = reinterpret_cast<const uint32_t *>(src + y * pitch);
                auto *dstRow = dst + y * dstStride;
                for (unsigned x = 0; x < renderW; ++x) {
                    const uint32_t c = srcRow[x];
                    const uint32_t r = (c >> 16) & 0xFF;
                    const uint32_t g = (c >> 8) & 0xFF;
                    const uint32_t b_val = c & 0xFF;
                    dstRow[x] = 0xFF000000 | (b_val << 16) | (g << 8) | r;
                }
            }
        }
    }

    ANativeWindow_unlockAndPost(g_native_window);
}

static constexpr size_t AUDIO_RING_CAPACITY = 44100 * 4; // 2 seconds stereo PCM (44.1kHz)
static int16_t g_audio_ring[AUDIO_RING_CAPACITY];
static size_t g_audio_write_idx = 0;
static size_t g_audio_read_idx = 0;
static size_t g_audio_available = 0;
static std::mutex g_audio_mutex;
static float g_audio_volume = 1.0f;
static bool g_audio_muted = false;

static size_t retro_audio_sample_batch_cb(const int16_t *data, size_t frames) {
    if (!data || frames == 0) return frames;
    if (g_audio_muted || g_audio_volume <= 0.001f) return frames;

    std::lock_guard<std::mutex> lock(g_audio_mutex);
    const size_t total_samples = frames * 2;
    for (size_t i = 0; i < total_samples; ++i) {
        int32_t val = static_cast<int32_t>(data[i] * g_audio_volume);
        if (val > 32767) val = 32767;
        else if (val < -32768) val = -32768;

        g_audio_ring[g_audio_write_idx] = static_cast<int16_t>(val);
        g_audio_write_idx = (g_audio_write_idx + 1) % AUDIO_RING_CAPACITY;
        if (g_audio_available < AUDIO_RING_CAPACITY) {
            g_audio_available++;
        } else {
            g_audio_read_idx = (g_audio_read_idx + 1) % AUDIO_RING_CAPACITY;
        }
    }
    return frames;
}

static void retro_audio_sample_cb(int16_t left, int16_t right) {
    int16_t stereo[2] = { left, right };
    retro_audio_sample_batch_cb(stereo, 1);
}
static void retro_input_poll_cb(void) {}

static int16_t retro_input_state_cb(unsigned port, unsigned device, unsigned, unsigned id) {
    if (device != RETRO_DEVICE_JOYPAD) return 0;
    const uint16_t m = port == 0 ? g_p1_input_mask : g_p2_input_mask;
    switch (id) {
        case RETRO_DEVICE_ID_JOYPAD_B: return (m & (1 << 0)) ? 1 : 0; // Cross
        case RETRO_DEVICE_ID_JOYPAD_A: return (m & (1 << 1)) ? 1 : 0; // Circle
        case RETRO_DEVICE_ID_JOYPAD_Y: return (m & (1 << 2)) ? 1 : 0; // Square
        case RETRO_DEVICE_ID_JOYPAD_X: return (m & (1 << 3)) ? 1 : 0; // Triangle
        case RETRO_DEVICE_ID_JOYPAD_L: return (m & (1 << 4)) ? 1 : 0; // L1
        case RETRO_DEVICE_ID_JOYPAD_R: return (m & (1 << 5)) ? 1 : 0; // R1
        case RETRO_DEVICE_ID_JOYPAD_L2: return (m & (1 << 6)) ? 1 : 0; // L2
        case RETRO_DEVICE_ID_JOYPAD_R2: return (m & (1 << 7)) ? 1 : 0; // R2
        case RETRO_DEVICE_ID_JOYPAD_SELECT: return (m & (1 << 8)) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_START: return (m & (1 << 9)) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_UP: return (m & (1 << 12)) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_DOWN: return (m & (1 << 13)) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_LEFT: return (m & (1 << 14)) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_RIGHT: return (m & (1 << 15)) ? 1 : 0;
        default: return 0;
    }
}

static bool retro_environment_cb(unsigned cmd, void *data) {
    if (!data && cmd != RETRO_ENVIRONMENT_SHUTDOWN) return false;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if (g_system_directory.empty()) return false;
            *static_cast<const char **>(data) = g_system_directory.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if (g_save_directory.empty()) return false;
            *static_cast<const char **>(data) = g_save_directory.c_str();
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            g_pixel_format = *static_cast<enum retro_pixel_format *>(data);
            return g_pixel_format == RETRO_PIXEL_FORMAT_RGB565 ||
                   g_pixel_format == RETRO_PIXEL_FORMAT_0RGB1555 ||
                   g_pixel_format == RETRO_PIXEL_FORMAT_XRGB8888;
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool *>(data) = true;
            return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            auto *cb = static_cast<retro_log_callback *>(data);
            cb->log = retro_log_cb;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            auto *var = static_cast<retro_variable *>(data);
            if (!var || !var->key) return false;
            if (strcmp(var->key, "pcsx_rearmed_async_cd") == 0) {
                var->value = "sync";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_frameskip") == 0) {
                var->value = "0";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_dithering") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_region") == 0) {
                var->value = "auto";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_spu_interpolation") == 0) {
                var->value = "simple";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_pad1type") == 0 || strcmp(var->key, "pcsx_rearmed_pad2type") == 0) {
                var->value = "standard";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_multitap1") == 0 || strcmp(var->key, "pcsx_rearmed_multitap2") == 0) {
                var->value = "disabled";
                return true;
            }
            if (strcmp(var->key, "pcsx_rearmed_psxclock") == 0) {
                var->value = "57";
                return true;
            }
            var->value = nullptr;
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE_EXT:
            *static_cast<int *>(data) = 1 | 2; // Enable both Video & Audio
            return true;
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_GET_OVERSCAN:
            return true;
        default:
            return false;
    }
}

static void ensure_directory(const std::string &p) {
    if (!p.empty()) mkdir(p.c_str(), 0700);
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSetDirectories(
        JNIEnv *env, jobject, jstring system_path, jstring save_path) {
    if (!system_path || !save_path) { set_error("system/save path is null"); return JNI_FALSE; }
    const char *s = env->GetStringUTFChars(system_path, nullptr);
    const char *v = env->GetStringUTFChars(save_path, nullptr);
    if (!s || !v) {
        if (s) env->ReleaseStringUTFChars(system_path, s);
        if (v) env->ReleaseStringUTFChars(save_path, v);
        set_error("could not read system/save path");
        return JNI_FALSE;
    }
    g_system_directory = s;
    g_save_directory = v;
    env->ReleaseStringUTFChars(system_path, s);
    env->ReleaseStringUTFChars(save_path, v);
    ensure_directory(g_system_directory);
    ensure_directory(g_save_directory);
    LOGI("system=%s saves=%s", g_system_directory.c_str(), g_save_directory.c_str());
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeLoadCore(
        JNIEnv *env, jobject, jstring core_path) {
    if (!core_path) { set_error("core path is null"); return JNI_FALSE; }
    if (g_core_dl_handle) return g_core_initialized ? JNI_TRUE : JNI_FALSE;

    const char *p = env->GetStringUTFChars(core_path, nullptr);
    if (!p) { set_error("could not read core path"); return JNI_FALSE; }
    const std::string cp(p);
    env->ReleaseStringUTFChars(core_path, p);

    struct stat st{};
    if (stat(cp.c_str(), &st) != 0 || st.st_size <= 0) {
        set_error("PS1 core file is missing or empty: " + cp);
        return JNI_FALSE;
    }

    dlerror();
    g_core_dl_handle = dlopen(cp.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!g_core_dl_handle) {
        const char *err = dlerror();
        set_error(std::string("dlopen failed: ") + (err ? err : "unknown linker error"));
        return JNI_FALSE;
    }

    g_retro_init = (retro_init_t)dlsym(g_core_dl_handle, "retro_init");
    g_retro_deinit = (retro_deinit_t)dlsym(g_core_dl_handle, "retro_deinit");
    g_retro_load_game = (retro_load_game_t)dlsym(g_core_dl_handle, "retro_load_game");
    g_retro_unload_game = (retro_unload_game_t)dlsym(g_core_dl_handle, "retro_unload_game");
    g_retro_run = (retro_run_t)dlsym(g_core_dl_handle, "retro_run");
    g_retro_serialize_size = (retro_serialize_size_t)dlsym(g_core_dl_handle, "retro_serialize_size");
    g_retro_serialize = (retro_serialize_t)dlsym(g_core_dl_handle, "retro_serialize");
    g_retro_unserialize = (retro_unserialize_t)dlsym(g_core_dl_handle, "retro_unserialize");
    auto se = (retro_set_environment_t)dlsym(g_core_dl_handle, "retro_set_environment");
    auto sv = (retro_set_video_refresh_t)dlsym(g_core_dl_handle, "retro_set_video_refresh");
    auto sa = (retro_set_audio_sample_t)dlsym(g_core_dl_handle, "retro_set_audio_sample");
    auto sb = (retro_set_audio_sample_batch_t)dlsym(g_core_dl_handle, "retro_set_audio_sample_batch");
    auto si = (retro_set_input_state_t)dlsym(g_core_dl_handle, "retro_set_input_state");
    auto sp = (retro_set_input_poll_t)dlsym(g_core_dl_handle, "retro_set_input_poll");

    if (!g_retro_init || !g_retro_deinit || !g_retro_load_game || !g_retro_unload_game ||
        !g_retro_run || !se || !sv || !si || !sp) {
        set_error("PS1 core is incompatible: required Libretro symbols are missing");
        dlclose(g_core_dl_handle);
        g_core_dl_handle = nullptr;
        return JNI_FALSE;
    }

    se(retro_environment_cb);
    sv(retro_video_refresh_cb);
    if (sa) sa(retro_audio_sample_cb);
    if (sb) sb(retro_audio_sample_batch_cb);
    si(retro_input_state_cb);
    sp(retro_input_poll_cb);
    g_retro_init();
    g_core_initialized = true;
    g_last_error.clear();
    LOGI("PS1 core initialized: %s", cp.c_str());
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeLoadGame(
        JNIEnv *env, jobject, jstring game_path) {
    if (!g_core_initialized || !g_retro_load_game || !game_path) {
        set_error("PS1 core is not initialized");
        return JNI_FALSE;
    }
    const char *p = env->GetStringUTFChars(game_path, nullptr);
    if (!p) { set_error("could not read game path"); return JNI_FALSE; }
    const std::string gp(p);
    env->ReleaseStringUTFChars(game_path, p);

    struct stat st{};
    if (stat(gp.c_str(), &st) != 0 || st.st_size <= 0) {
        set_error("game image is missing or empty: " + gp);
        return JNI_FALSE;
    }

    retro_game_info info{};
    info.path = gp.c_str();
    const bool ok = g_retro_load_game(&info);
    g_game_loaded = ok;
    if (!ok) {
        set_error("Libretro rejected the PS1 image: " + gp);
        return JNI_FALSE;
    }
    g_last_error.clear();
    LOGI("PS1 game loaded: %s (%lld bytes)", gp.c_str(), static_cast<long long>(st.st_size));
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeGetLastError(JNIEnv *env, jobject) {
    return env->NewStringUTF(g_last_error.c_str());
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeRunFrame(JNIEnv*, jobject, jint p1, jint p2) {
    if (!g_core_initialized || !g_game_loaded || !g_retro_run) return;
    g_p1_input_mask = static_cast<uint16_t>(p1);
    g_p2_input_mask = static_cast<uint16_t>(p2);
    g_retro_run();
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeUnloadGame(JNIEnv*, jobject) {
    if (g_game_loaded && g_retro_unload_game) g_retro_unload_game();
    if (g_core_initialized && g_retro_deinit) g_retro_deinit();
    g_game_loaded = false;
    g_core_initialized = false;
    if (g_core_dl_handle) dlclose(g_core_dl_handle);
    g_core_dl_handle = nullptr;
    g_retro_init = nullptr;
    g_retro_deinit = nullptr;
    g_retro_load_game = nullptr;
    g_retro_unload_game = nullptr;
    g_retro_run = nullptr;
    g_retro_serialize_size = nullptr;
    g_retro_serialize = nullptr;
    g_retro_unserialize = nullptr;

    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (g_native_window) {
        ANativeWindow_release(g_native_window);
        g_native_window = nullptr;
    }
    g_last_window_w = 0;
    g_last_window_h = 0;
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSetSurface(JNIEnv *env, jobject, jobject surface) {
    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (g_native_window) {
        ANativeWindow_release(g_native_window);
        g_native_window = nullptr;
    }
    g_last_window_w = 0;
    g_last_window_h = 0;
    if (surface) {
        g_native_window = ANativeWindow_fromSurface(env, surface);
    }
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSetInput(JNIEnv*, jobject, jint p1, jint p2) {
    g_p1_input_mask = static_cast<uint16_t>(p1);
    g_p2_input_mask = static_cast<uint16_t>(p2);
}

JNIEXPORT jbyteArray JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSaveState(JNIEnv *env, jobject) {
    if (!g_core_initialized || !g_game_loaded || !g_retro_serialize_size || !g_retro_serialize) return nullptr;
    const size_t size = g_retro_serialize_size();
    if (size == 0) return nullptr;
    std::vector<uint8_t> bytes(size);
    if (!g_retro_serialize(bytes.data(), size)) return nullptr;
    jbyteArray result = env->NewByteArray(static_cast<jsize>(size));
    if (result) env->SetByteArrayRegion(result, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte *>(bytes.data()));
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeLoadState(JNIEnv *env, jobject, jbyteArray state) {
    if (!g_core_initialized || !g_game_loaded || !g_retro_unserialize || !state) return JNI_FALSE;
    const jsize size = env->GetArrayLength(state);
    if (size <= 0) return JNI_FALSE;
    std::vector<uint8_t> bytes(static_cast<size_t>(size));
    env->GetByteArrayRegion(state, 0, size, reinterpret_cast<jbyte *>(bytes.data()));
    return g_retro_unserialize(bytes.data(), static_cast<size_t>(size)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSetAudioVolume(JNIEnv *, jobject, jfloat volume) {
    if (volume < 0.0f) volume = 0.0f;
    if (volume > 1.0f) volume = 1.0f;
    g_audio_volume = volume;
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeSetAudioMute(JNIEnv *, jobject, jboolean muted) {
    g_audio_muted = (muted == JNI_TRUE);
}

JNIEXPORT jint JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeReadAudio(JNIEnv *env, jobject, jshortArray out_buffer, jint max_samples) {
    if (!out_buffer || max_samples <= 0) return 0;

    std::lock_guard<std::mutex> lock(g_audio_mutex);
    if (g_audio_available == 0) return 0;

    const size_t count = std::min(static_cast<size_t>(max_samples), g_audio_available);
    jshort *dst = env->GetShortArrayElements(out_buffer, nullptr);
    if (!dst) return 0;

    for (size_t i = 0; i < count; ++i) {
        dst[i] = g_audio_ring[g_audio_read_idx];
        g_audio_read_idx = (g_audio_read_idx + 1) % AUDIO_RING_CAPACITY;
    }
    g_audio_available -= count;

    env->ReleaseShortArrayElements(out_buffer, dst, 0);
    return static_cast<jint>(count);
}

JNIEXPORT void JNICALL Java_com_ps1_netplay_core_NativeCoreBridge_nativeResetAudio(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_audio_mutex);
    g_audio_write_idx = 0;
    g_audio_read_idx = 0;
    g_audio_available = 0;
}

} // extern "C"

