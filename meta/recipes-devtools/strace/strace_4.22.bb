SUMMARY = "System call tracing tool"
HOMEPAGE = "http://strace.io"
SECTION = "console/utils"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://COPYING;md5=6ddb91734b9c705f3e87362e97e5f64b"

SRC_URI = "https://strace.io/files/${PV}/strace-${PV}.tar.xz \
           file://disable-git-version-gen.patch \
           file://more-robust-test-for-m32-mx32-compile-support.patch \
           file://update-gawk-paths.patch \
           file://Makefile-ptest.patch \
           file://run-ptest \
           file://0001-Fix-build-when-using-non-glibc-libc-implementation-o.patch \
           file://mips-SIGEMT.patch \
           file://0001-caps-abbrev.awk-fix-gawk-s-path.patch \
           file://0001-tests-sigaction-Check-for-mips-and-alpha-before-usin.patch \
           "
SRC_URI[md5sum] = "7a2a7d7715da6e6834bc65bd09bace1c"
SRC_URI[sha256sum] = "068cd09264c95e4d591bbcd3ea08f99a693ed8663cd5169b0fdad72eb5bdb39d"

inherit autotools ptest bluetooth

EXTRA_OECONF += "--enable-mpers=no"

CFLAGS_append_libc-musl = " -Dsigcontext_struct=sigcontext"
# otherwise strace-4.22/tests/inject-nf.c fails to build as discussed here:
# http://lists.openembedded.org/pipermail/openembedded-core/2018-May/150647.html
DEBUG_OPTIMIZATION_remove = "${@bb.utils.contains('PTEST_ENABLED', '1', '-fno-omit-frame-pointer', '', d)}"

RDEPENDS_${PN}-ptest += "make coreutils grep gawk sed"

PACKAGECONFIG_class-target ??= "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'bluetooth', 'bluez', '', d)} \
"

PACKAGECONFIG[bluez] = "ac_cv_header_bluetooth_bluetooth_h=yes,ac_cv_header_bluetooth_bluetooth_h=no,${BLUEZ}"
PACKAGECONFIG[libunwind] = "--with-libunwind,--without-libunwind,libunwind"

TESTDIR = "tests"

do_install_append() {
	# We don't ship strace-graph here because it needs perl
	rm ${D}${bindir}/strace-graph
}

do_compile_ptest() {
	oe_runmake -C ${TESTDIR} buildtest-TESTS
}

do_install_ptest() {
	oe_runmake -C ${TESTDIR} install-ptest BUILDDIR=${B} DESTDIR=${D}${PTEST_PATH} TESTDIR=${TESTDIR}
	sed -i -e '/^src/s/strace.*[1-9]/ptest/' \
	    -e 's,--sysroot=${STAGING_DIR_TARGET},,g' \
	    -e 's|${DEBUG_PREFIX_MAP}||g' \
	    -e 's:${HOSTTOOLS_DIR}/::g' \
	    -e 's:${RECIPE_SYSROOT_NATIVE}::g' \
	    -e 's:${RECIPE_SYSROOT}::g' \
	    -e 's:${BASE_WORKDIR}/${MULTIMACH_TARGET_SYS}::g' \
	    -e '/^DEB_CHANGELOGTIME/d' \
	    -e '/^RPM_CHANGELOGTIME/d' \
	${D}/${PTEST_PATH}/${TESTDIR}/Makefile
}

BBCLASSEXTEND = "native"
TOOLCHAIN = "gcc"
