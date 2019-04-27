#!/bin/bash -
# This is a helper script to download and build the correct version of llvm+clang
# for the project. It should work on most Linux and MacOSX.

die() {
    echo "error: $1" >&2
    exit 1
}

die_svn() {
    svn cleanup "$2" > /dev/null 2>&1
    echo "error: $1" >&2
    exit 1
}

need_cmd() {
    if ! command -v "$1" > /dev/null 2>&1; then
        die "required command not found: '$1'"
    fi
}

need_cmd mkdir
need_cmd svn
need_cmd gcc
need_cmd cmake
need_cmd ninja
need_cmd md5sum
need_cmd patch

LLVM_VER=401
LLVM_URL=https://llvm.org/svn/llvm-project

TOOLCHAIN_DIR=${HOME}/toolchains
TOOLCHAIN_BLD=${TOOLCHAIN_DIR}/build
TOOLCHAIN_NAME=llvm+clang-${LLVM_VER}-$(gcc -dumpmachine)

SRC_DIR=${TOOLCHAIN_BLD}/${TOOLCHAIN_NAME}
BLD_DIR=${TOOLCHAIN_BLD}/${TOOLCHAIN_NAME}_build
INSTALL_DIR=${TOOLCHAIN_DIR}/${TOOLCHAIN_NAME}

# get sources
mkdir -p "${SRC_DIR}"
svn co "${LLVM_URL}/llvm/tags/RELEASE_${LLVM_VER}/final/" "${SRC_DIR}"             || die_svn "unable to get llvm svn repository"  "${SRC_DIR}"
svn co "${LLVM_URL}/cfe/tags/RELEASE_${LLVM_VER}/final/"  "${SRC_DIR}/tools/clang" || die_svn "unable to get clang svn repository" "${SRC_DIR}/tools/clang"

#
# fix build with gcc 8.x see https://bugzilla.redhat.com/show_bug.cgi?id=1540620
#
TOFIX_FILE=${SRC_DIR}/include/llvm/ExecutionEngine/Orc/OrcRemoteTargetClient.h
TOFIX_HASH=d4838c4f039953245e1b0a4b776ae74a

IFS='' read -r -d '' PATCH_FIX_STR <<"EOF"
@@ -713,8 +713,8 @@ private:
 
   uint32_t getTrampolineSize() const { return RemoteTrampolineSize; }
 
-  Expected<std::vector<char>> readMem(char *Dst, JITTargetAddress Src,
-                                      uint64_t Size) {
+  Expected<std::vector<uint8_t>> readMem(char *Dst, JITTargetAddress Src,
+                                         uint64_t Size) {
     // Check for an 'out-of-band' error, e.g. from an MM destructor.
     if (ExistingError)
       return std::move(ExistingError);
EOF

if md5sum -c - <<< "${TOFIX_HASH} ${TOFIX_FILE}" > /dev/null 2>&1 ; then
    patch -b "${TOFIX_FILE}" <<< "${PATCH_FIX_STR}" || die "failed to patch 'OrcRemoteTargetClient.h'"
fi

#
# keep C code variables/args names even with Release build
#
TOPATCH_FILE=${SRC_DIR}/tools/clang/lib/Driver/Tools.cpp
TOPATCH_HASH=2fa3a8c9a0ee8e0d6560a95f360f60d5

IFS='' read -r -d '' PATCH_VNAMES_STR <<"EOF"
@@ -4277,8 +4277,6 @@
 // Disable the verification pass in -asserts builds.
 #ifdef NDEBUG
   CmdArgs.push_back("-disable-llvm-verifier");
-  // Discard LLVM value names in -asserts builds.
-  CmdArgs.push_back("-discard-value-names");
 #endif
 
   // Set the main file name, so that debug info works even with
EOF

if md5sum -c - <<< "${TOPATCH_HASH} ${TOPATCH_FILE}" > /dev/null 2>&1 ; then
    patch -b "${TOPATCH_FILE}" <<< "${PATCH_VNAMES_STR}" || die "failed to patch 'Tools.cpp'"
fi

# configure build
export CMAKE_C_FLAGS=" -pthread -lpthread -ldl"
export LD_FLAGS=" -pthread -lpthread -ldl"
export CMAKE_CXX_FLAGS=" -pthread -lpthread -ldl"
mkdir -p "${BLD_DIR}"
cd "${BLD_DIR}" && cmake -G "Ninja"                              \
                         -Wno-dev                                \
                         -DCMAKE_BUILD_TYPE=Release              \
                         -DLLVM_DYLIB_EXPORT_ALL=ON              \
                         -DLLVM_TARGETS_TO_BUILD="host"          \
                         -DLLVM_TARGET_ARCH="host"               \
                         -DBUILD_SHARED_LIBS=ON                  \
                         -DCMAKE_INSTALL_PREFIX="${INSTALL_DIR}" \
                         -DLLVM_INCLUDE_TESTS=Off                \
                         "${SRC_DIR}"

# build llvm and clang
# CMAKE_C_FLAGS:-pthread
# CMAKE_CXX_FLAGS:-pthread
cmake --build "${BLD_DIR}"                  || die "unable to build llvm/clang"
cmake --build "${BLD_DIR}" --target install || die "unable to install llvm/clang"
