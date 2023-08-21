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

## The following stakeholders are defined:
##
## role: application-specific behaviour in responding to FastCGI
## requests
##
## application: deployment-specific behaviour in starting up a FastCGI
## application
##
## transport: connection-specific behaviour in communicating between
## server and FastCGI application (e.g., stand-alone vs forked,
## UNIX-domain vs INET-domain, etc)
##
## engine: behaviour connecting protocol messages to roles
##
## deployment: invocation, not necessarily any development
##
## The various source trees are designed to minimize dependencies for
## developers operating in a single role.  There's usually room for
## improvement.

## These classes have some general utility in building FastCGI
## applications, but have no dependency on them.  They include MIME
## multipart support.  Stakeholders: none, but potentially useful to
## all
SELECTED_JARS += fastcgi4j_util
trees_fastcgi4j_util += util
roots_util += $(found_util)

## These classes define the FastCGI roles and their contexts.  Role
## and framework implementations need these.  Stakeholders: role,
## engine
SELECTED_JARS += fastcgi4j_contract
trees_fastcgi4j_contract += contract
roots_contract += $(found_contract)

## These classes make use of role contexts, and help in the
## implementation of roles.  Stakeholders: role
SELECTED_JARS += fastcgi4j_api
trees_fastcgi4j_api += api
roots_api += $(found_api)
deps_api += contract
deps_api += util

## These classes define the engine and transport frameworks.  They are
## of use to application implementations, and bind together role
## implementations with engines and transports.  Stakeholders: engine,
## application, transport
SELECTED_JARS += fastcgi4j_app
trees_fastcgi4j_app += app
roots_app += $(found_app)
deps_app += contract

## These provide an abstraction of thread provision, so that soft
## thread/virtual thread/fibres can be used when available.
## Stakeholders: engine, deployment
SELECTED_JARS += fastcgi4j_threads
trees_fastcgi4j_threads += threads
roots_threads += $(found_threads)
deps_threads += app

## These provide soft threads automatically when included in the
## classpath.  Stakeholders: deployment
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_SOFT))),)
SELECTED_JARS += fastcgi4j_soft
trees_fastcgi4j_soft += soft
roots_soft += $(found_soft)
deps_soft += app
deps_soft += threads
endif

## These are engine plugins.  Stakeholders: deployment
SELECTED_JARS += fastcgi4j_engine
trees_fastcgi4j_engine += engine
roots_engine += $(found_engine)
deps_engine += contract
deps_engine += app
deps_engine += proto
deps_engine += threads
deps_engine += util

## These classes serialize and de-serialize FastCGI protocol messages.
## Stakeholders: engine
SELECTED_JARS += fastcgi4j_proto
trees_fastcgi4j_proto += proto
roots_proto += $(found_proto)

## This is the stand-alone Internet-domain transport plugin.
## Stakeholders: deployment
SELECTED_JARS += fastcgi4j_inet
trees_fastcgi4j_inet += inet
roots_inet += $(found_inet)
deps_inet += app
deps_inet += proto

## This is the inherited-channel transport plugin.  Stakeholders:
## deployment
SELECTED_JARS += fastcgi4j_inherit
trees_fastcgi4j_inherit += inherit
roots_inherit += $(found_inherit)
deps_inherit += app
deps_inherit += proto

## This transport plugin was intended to allow server-forked
## applications, but it is obviated by the inherited-channel
## transport.  Stakeholders: deployment
# ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
# SELECTED_JARS += fastcgi4j_fork
# trees_fastcgi4j_fork += fork
# endif

## This is the stand-alone UNIX-domain transport plugin.
## Stakeholders: deployment
SELECTED_JARS += fastcgi4j_unix
trees_fastcgi4j_unix += unix
roots_unix += $(found_unix)
deps_unix += app
deps_unix += proto

## This is the IIS transport plugin.  Stakeholders: deployment
SELECTED_JARS += fastcgi4j_iis
trees_fastcgi4j_iis += iis
roots_iis += $(found_iis)
deps_iis += app
deps_iis += proto

## These are demonstration applications.  Stakeholders: role,
## application
SELECTED_JARS += fastcgi4j_demos
trees_fastcgi4j_demos += demos
roots_demos += $(found_demos)
deps_demos += contract
deps_demos += api
deps_demos += app
deps_demos += util

## These are unit tests.  Stakeholders: engine, transport
jars += tests
roots_tests += $(found_tests)
deps_tests += contract
deps_tests += api
deps_tests += app
deps_tests += util

jars += $(SELECTED_JARS)

hidden_binaries.c += bindwrap
bindwrap_obj += bindwrap

# ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
# hidden_libraries += main
# endif
# main_libname = fastcgi4j
# main_mod += native
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
DOC_PKGS += uk.ac.lancs.fastcgi.io
DOC_PKGS += uk.ac.lancs.fastcgi.mime
DOC_PKGS += uk.ac.lancs.fastcgi.path
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
# ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
# DOC_PKGS += uk.ac.lancs.fastcgi.transport.fork
# endif

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

# $(BINODEPS_OBJDIR)/native.lo: | tmp/tree-unix.compiled

all:: VERSION BUILD installed-jars
all:: installed-binaries
installed-jars:: $(SELECTED_JARS:%=out/%.jar)
installed-jars:: $(SELECTED_JARS:%=out/%-src.zip)

define JARVERSION
version_$1=$$(VERSION)

endef


$(foreach j,$(jars),$(eval $(call JARVERSION,$j)))

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install:: install-jars install-scripts
install:: install-hidden-binaries

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
YEARS=2022,2023

update-licence:
	$(FIND) . -name ".git" -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright (c)\s[-0-9,]\+\sLancaster University/Copyright (c) $(YEARS), Lancaster University/g'
