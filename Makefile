all::

PREFIX=/usr/local
SED ?= sed
FIND ?= find
XARGS ?= xargs
CMP ?= cmp -s
CP ?= cp

ENABLE_SOFT ?= yes
#ENABLE_UNIX ?= yes

VWORDS:=$(shell src/getversion.sh --prefix=v MAJOR MINOR PATCH)
VERSION:=$(word 1,$(VWORDS))
BUILD:=$(word 2,$(VWORDS))

## Provide a version of $(abspath) that can cope with spaces in the
## current directory.
myblank:=
myspace:=$(myblank) $(myblank)
MYCURDIR:=$(subst $(myspace),\$(myspace),$(CURDIR)/)
MYABSPATH=$(foreach f,$1,$(if $(patsubst /%,,$f),$(MYCURDIR)$f,$f))

-include $(call MYABSPATH,config.mk)
-include fastcgi4j-env.mk

lc=$(subst A,a,$(subst B,b,$(subst C,c,$(subst D,d,$(subst E,e,$(subst F,f,$(subst G,g,$(subst H,h,$(subst I,i,$(subst J,j,$(subst K,k,$(subst L,l,$(subst M,m,$(subst N,n,$(subst O,o,$(subst P,p,$(subst Q,q,$(subst R,r,$(subst S,s,$(subst T,t,$(subst U,u,$(subst V,v,$(subst W,w,$(subst X,x,$(subst Y,y,$(subst Z,z,$1))))))))))))))))))))))))))

JARDEPS_SRCDIR=src/java
JARDEPS_DEPDIR=src
JARDEPS_MERGEDIR=src/merge

SELECTED_JARS += fastcgi4j_api
trees_fastcgi4j_api += api

SELECTED_JARS += fastcgi4j_app
trees_fastcgi4j_app += app

SELECTED_JARS += fastcgi4j_threads
trees_fastcgi4j_threads += threads

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_SOFT))),)
SELECTED_JARS += fastcgi4j_soft
trees_fastcgi4j_soft += soft
endif

SELECTED_JARS += fastcgi4j_engine
trees_fastcgi4j_engine += engine

SELECTED_JARS += fastcgi4j_proto
trees_fastcgi4j_proto += proto

SELECTED_JARS += fastcgi4j_inet
trees_fastcgi4j_inet += inet

SELECTED_JARS += fastcgi4j_inherit
trees_fastcgi4j_inherit += inherit

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
SELECTED_JARS += fastcgi4j_fork
trees_fastcgi4j_fork += fork
endif

SELECTED_JARS += fastcgi4j_unix
trees_fastcgi4j_unix += unix

SELECTED_JARS += fastcgi4j_iis
trees_fastcgi4j_iis += iis

jars += $(SELECTED_JARS)

SELECTED_JARS += fastcgi4j_demos
trees_fastcgi4j_demos += demos

jars += tests

roots_api += $(found_api)
roots_app += $(found_app)
deps_app += api
roots_threads += $(found_threads)
deps_threads += app
roots_soft += $(found_soft)
deps_soft += app
deps_soft += threads
roots_engine += $(found_engine)
deps_engine += api
deps_engine += app
deps_engine += proto
deps_engine += threads
roots_proto += $(found_proto)
roots_unix += $(found_unix)
deps_unix += app
deps_unix += proto
roots_inet += $(found_inet)
deps_inet += app
deps_inet += proto
roots_inherit += $(found_inherit)
deps_inherit += app
deps_inherit += proto
roots_iis += $(found_iis)
deps_iis += app
deps_iis += proto
roots_demos += $(found_demos)
deps_demos += api
deps_demos += app
roots_tests += $(found_tests)
deps_tests += api
deps_tests += app

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
hidden_libraries += main
endif
main_libname = fastcgi4j
main_mod += native
scripts += fastcgi4j

SHAREDIR ?= $(PREFIX)/share/fastcgi4j
LIBEXECDIR ?= $(PREFIX)/libexec/fastcgi4j

include binodeps.mk

include jardeps.mk
-include jardeps-install.mk

DOC_OVERVIEW=src/java/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(SELECTED_JARS))
DOC_CORE=fastcgi4j
DOC_PKGS += uk.ac.lancs.fastcgi
DOC_PKGS += uk.ac.lancs.fastcgi.context
DOC_PKGS += uk.ac.lancs.fastcgi.util
DOC_PKGS += uk.ac.lancs.fastcgi.app
DOC_PKGS += uk.ac.lancs.fastcgi.engine
DOC_PKGS += uk.ac.lancs.fastcgi.engine.util
DOC_PKGS += uk.ac.lancs.fastcgi.engine.std
DOC_PKGS += uk.ac.lancs.fastcgi.engine.std.threading
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_SOFT))),)
DOC_PKGS += uk.ac.lancs.fastcgi.engine.std.threading.soft
endif
DOC_PKGS += uk.ac.lancs.fastcgi.proto
DOC_PKGS += uk.ac.lancs.fastcgi.proto.serial
DOC_PKGS += uk.ac.lancs.fastcgi.transport
DOC_PKGS += uk.ac.lancs.fastcgi.transport.inet
DOC_PKGS += uk.ac.lancs.fastcgi.transport.iis
DOC_PKGS += uk.ac.lancs.fastcgi.transport.unix
DOC_PKGS += uk.ac.lancs.fastcgi.transport.inherit
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
DOC_PKGS += uk.ac.lancs.fastcgi.transport.fork
endif

MYCMPCP=$(CMP) -s '$1' '$2' || $(CP) '$1' '$2'
.PHONY: prepare-version
mktmp:
	@$(MKDIR) tmp/
prepare-version: mktmp
	$(file >tmp/BUILD,$(BUILD))
	$(file >tmp/VERSION,$(VERSION))
BUILD: prepare-version
	@$(call MYCMPCP,tmp/BUILD,$@)
VERSION: prepare-version
	@$(call MYCMPCP,tmp/VERSION,$@)

$(BINODEPS_OBJDIR)/native.lo: | tmp/tree-unix.compiled

all:: VERSION BUILD installed-jars installed-libraries
installed-jars:: $(SELECTED_JARS:%=out/%.jar)
installed-jars:: $(SELECTED_JARS:%=out/%-src.zip)

define JARVERSION
version_$1=$$(VERSION)

endef


$(foreach j,$(jars),$(eval $(call JARVERSION,$j)))

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install:: install-jars install-hidden-libraries install-scripts

tidy::
	@$(PRINTF) 'Deleting trash\n'
	@$(FIND) . -name "*~" -delete

clean:: tidy

distclean:: blank
	$(RM) VERSION BUILD

test_suite += uk.ac.lancs.fastcgi.engine.util.TestCachePipePool

jtests: $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
	@for class in $(test_suite) ; do \
	  $(PRINTF) 'Testing %s\n' "$$class"; \
	  $(JAVA) -ea -cp $(subst $(jardeps_space),:,$(jars:%=$(JARDEPS_OUTDIR)/%.jar):$(CLASSPATH)) \
	  junit.textui.TestRunner $${class} ; \
	done



# Set this to the comma-separated list of years that should appear in
# the licence.  Do not use characters other than [-0-9,] - no spaces.
YEARS=2022

update-licence:
	$(FIND) . -name ".git" -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright (c)\s[-0-9,]\+\sLancaster University/Copyright (c) $(YEARS), Lancaster University/g'
