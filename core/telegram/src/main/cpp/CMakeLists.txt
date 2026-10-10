cmake_minimum_required(VERSION 3.22)
project(TGPhotosTdJson LANGUAGES CXX)
if(TGPHOTOS_TDLIB_SOURCE_DIR AND TGPHOTOS_TDLIB_PREBUILT_JSON)
    if(NOT EXISTS "${TGPHOTOS_TDLIB_PREBUILT_JSON}")
        message(FATAL_ERROR "Verified TDLib JSON baseline is missing")
    endif()
    add_library(Td::TdJson SHARED IMPORTED)
    set_target_properties(Td::TdJson PROPERTIES
        IMPORTED_LOCATION "${TGPHOTOS_TDLIB_PREBUILT_JSON}"
        INTERFACE_INCLUDE_DIRECTORIES "${TGPHOTOS_TDLIB_SOURCE_DIR};${CMAKE_CURRENT_BINARY_DIR}")
    include(GenerateExportHeader)
    generate_export_header(Td::TdJson BASE_NAME tdjson
        EXPORT_FILE_NAME "${CMAKE_CURRENT_BINARY_DIR}/td/telegram/tdjson_export.h")
elseif(TGPHOTOS_TDLIB_SOURCE_DIR AND TGPHOTOS_TDLIB_BUILD_DIR)
    # CI builds the pinned official source; import its generated JSON target without rebuilding it.
    if(NOT EXISTS "${TGPHOTOS_TDLIB_BUILD_DIR}/td/libtdjson.so")
        message(FATAL_ERROR "Pinned TDLib JSON library is missing")
    endif()
    add_library(Td::TdJson SHARED IMPORTED)
    set_target_properties(Td::TdJson PROPERTIES
        IMPORTED_LOCATION "${TGPHOTOS_TDLIB_BUILD_DIR}/td/libtdjson.so"
        INTERFACE_INCLUDE_DIRECTORIES "${TGPHOTOS_TDLIB_SOURCE_DIR};${TGPHOTOS_TDLIB_BUILD_DIR}/td")
else()
    find_package(Td 1.8.68 REQUIRED)
endif()
add_library(tgphotos_tdjson SHARED tdjson_jni.cpp)
target_link_libraries(tgphotos_tdjson PRIVATE Td::TdJson)
target_compile_features(tgphotos_tdjson PRIVATE cxx_std_17)
if(NOT ANDROID)
    find_package(JNI REQUIRED)
    target_include_directories(tgphotos_tdjson PRIVATE ${JNI_INCLUDE_DIRS})
endif()
