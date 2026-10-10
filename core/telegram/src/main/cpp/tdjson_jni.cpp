#include <jni.h>
#include <algorithm>
#include <string>
#include <td/telegram/td_json_client.h>

namespace {
// Requests are JSON-escaped ASCII by the Kotlin writer where needed. Convert standard UTF-8 explicitly
// for returned JSON; JNI's NewStringUTF accepts modified UTF-8 and is not a general UTF-8 decoder.
jstring utf8_json(JNIEnv* env, const char* json) {
    if (json == nullptr) return nullptr;
    jclass string_class = env->FindClass("java/lang/String");
    if (string_class == nullptr) return nullptr;
    jmethodID constructor = env->GetMethodID(string_class, "<init>", "([BLjava/lang/String;)V");
    if (constructor == nullptr) return nullptr;
    const std::string bytes(json);
    jbyteArray array = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (array == nullptr) return nullptr;
    env->SetByteArrayRegion(array, 0, static_cast<jsize>(bytes.size()), reinterpret_cast<const jbyte*>(bytes.data()));
    jstring encoding = env->NewStringUTF("UTF-8");
    auto result = static_cast<jstring>(env->NewObject(string_class, constructor, array, encoding));
    env->DeleteLocalRef(array); env->DeleteLocalRef(encoding); env->DeleteLocalRef(string_class);
    return result;
}
std::string utf8_request(JNIEnv* env, jstring request) {
    jclass string_class = env->FindClass("java/lang/String");
    if (string_class == nullptr) return {};
    jmethodID get_bytes = env->GetMethodID(string_class, "getBytes", "(Ljava/lang/String;)[B");
    if (get_bytes == nullptr) return {};
    jstring encoding = env->NewStringUTF("UTF-8");
    auto array = static_cast<jbyteArray>(env->CallObjectMethod(request, get_bytes, encoding));
    if (env->ExceptionCheck() || array == nullptr) return {};
    const jsize length = env->GetArrayLength(array);
    std::string result(static_cast<size_t>(length), '\0');
    if (length > 0) env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte*>(&result[0]));
    env->DeleteLocalRef(array); env->DeleteLocalRef(encoding); env->DeleteLocalRef(string_class);
    return result;
}
}
extern "C" JNIEXPORT jint JNICALL
Java_app_tgphotos_core_telegram_NativeTdJson_createClient(JNIEnv*, jclass) { return td_create_client_id(); }
extern "C" JNIEXPORT void JNICALL
Java_app_tgphotos_core_telegram_NativeTdJson_send(JNIEnv* env, jclass, jint id, jstring request) {
    if (request == nullptr) return;
    std::string bytes = utf8_request(env, request);
    if (!env->ExceptionCheck()) td_send(id, bytes.c_str());
    // Best-effort clearing of this native copy. TDLib/Java may retain other copies until released.
    std::fill(bytes.begin(), bytes.end(), '\0');
}
extern "C" JNIEXPORT jstring JNICALL
Java_app_tgphotos_core_telegram_NativeTdJson_receive(JNIEnv* env, jclass, jdouble timeout) {
    if (timeout < 0 || timeout > 1) return nullptr;
    return utf8_json(env, td_receive(timeout));
}
extern "C" JNIEXPORT jstring JNICALL
Java_app_tgphotos_core_telegram_NativeTdJson_execute(JNIEnv* env, jclass, jstring request) {
    if (request == nullptr) return nullptr;
    std::string bytes = utf8_request(env, request);
    jstring result = env->ExceptionCheck() ? nullptr : utf8_json(env, td_execute(bytes.c_str()));
    std::fill(bytes.begin(), bytes.end(), '\0');
    return result;
}
