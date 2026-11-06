#include <sys/system_properties.h>
#include <cstring>
#include <cstdlib>
#include <pthread.h>
#include <jni.h>
#include "./xdl.h"
#include <android/log.h>
#include <dlfcn.h>
#include "Dobby/dobby.h"


#define LOG_TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Maximum number of spoofed properties per process
#define MAX_SPOOF_PROPS 32
#define PROP_KEY_LEN    92
#define PROP_VAL_LEN    92

struct SpoofedProp {
    char key[PROP_KEY_LEN];
    char value[PROP_VAL_LEN];
};

// Mutable per-process spoof table — empty until setSpoofedProps() is called
static SpoofedProp g_spoof_table[MAX_SPOOF_PROPS];
static int g_spoof_count = 0;
static pthread_mutex_t g_spoof_mutex = PTHREAD_MUTEX_INITIALIZER;

static int (*orig_system_property_get)(const char *name, char *value) = nullptr;


int my_system_property_get(const char *name, char *value) {
    pthread_mutex_lock(&g_spoof_mutex);
    for (int i = 0; i < g_spoof_count; ++i) {
        if (strcmp(name, g_spoof_table[i].key) == 0) {
            strncpy(value, g_spoof_table[i].value, PROP_VAL_LEN - 1);
            value[PROP_VAL_LEN - 1] = '\0';
            int len = (int)strlen(value);
            pthread_mutex_unlock(&g_spoof_mutex);
            LOGD("[spoof] %s = %s", name, value);
            return len;
        }
    }
    pthread_mutex_unlock(&g_spoof_mutex);

    if (orig_system_property_get) {
        return orig_system_property_get(name, value);
    }
    value[0] = '\0';
    return 0;
}

void install_property_get_hook() {
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    void* target = xdl_dsym(handle, "__system_property_get", nullptr);
    if (target) {
        if (DobbyHook(target, (void*)my_system_property_get, (void**)&orig_system_property_get) == 0) {
            LOGD("Spoof hook installed successfully");
        } else {
            LOGD("Spoof hook failed");
        }
        xdl_close(handle);
    } else {
        xdl_close(handle);
    }
}


__attribute__((constructor)) void init_virtual_spoof()
{
    install_property_get_hook();
    LOGD("VirtualSpoof: __system_property_get hook loaded (table empty, waiting for setSpoofedProps)");
}


/**
 * JNI: Called from NativeCore.setSpoofedProps(String[] keys, String[] values)
 * Populates the per-process spoof table. Should be called once before app init.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_tegufy_blackbox_core_NativeCore_setSpoofedProps(
        JNIEnv *env, jclass clazz, jobjectArray keys, jobjectArray values) {
    if (keys == nullptr || values == nullptr) return;

    jsize count = env->GetArrayLength(keys);
    if (count > MAX_SPOOF_PROPS) count = MAX_SPOOF_PROPS;

    pthread_mutex_lock(&g_spoof_mutex);
    g_spoof_count = 0;
    for (jsize i = 0; i < count; ++i) {
        auto jkey = (jstring) env->GetObjectArrayElement(keys, i);
        auto jval = (jstring) env->GetObjectArrayElement(values, i);
        if (jkey == nullptr || jval == nullptr) continue;

        const char* key = env->GetStringUTFChars(jkey, nullptr);
        const char* val = env->GetStringUTFChars(jval, nullptr);

        if (key && val) {
            strncpy(g_spoof_table[g_spoof_count].key, key, PROP_KEY_LEN - 1);
            g_spoof_table[g_spoof_count].key[PROP_KEY_LEN - 1] = '\0';
            strncpy(g_spoof_table[g_spoof_count].value, val, PROP_VAL_LEN - 1);
            g_spoof_table[g_spoof_count].value[PROP_VAL_LEN - 1] = '\0';
            g_spoof_count++;
            LOGD("[spoof] registered: %s = %s", key, val);
        }

        if (key) env->ReleaseStringUTFChars(jkey, key);
        if (val) env->ReleaseStringUTFChars(jval, val);
        env->DeleteLocalRef(jkey);
        env->DeleteLocalRef(jval);
    }
    pthread_mutex_unlock(&g_spoof_mutex);
    LOGD("[spoof] table updated: %d props", g_spoof_count);
}
