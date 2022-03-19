all::

PREFIX=/usr/local
SED ?= sed
FIND ?= find
XARGS ?= xargs

ENABLE_UNIX ?= yes

-include fastcgi4j-env.mk
-include $(subst $(jardeps_space),\$(jardeps_space),$(CURDIR))/config.mk

lc=$(subst A,a,$(subst B,b,$(subst C,c,$(subst D,d,$(subst E,e,$(subst F,f,$(subst G,g,$(subst H,h,$(subst I,i,$(subst J,j,$(subst K,k,$(subst L,l,$(subst M,m,$(subst N,n,$(subst O,o,$(subst P,p,$(subst Q,q,$(subst R,r,$(subst S,s,$(subst T,t,$(subst U,u,$(subst V,v,$(subst W,w,$(subst X,x,$(subst Y,y,$(subst Z,z,$1))))))))))))))))))))))))))

JARDEPS_SRCDIR=src/java
JARDEPS_DEPDIR=src
JARDEPS_MERGEDIR=src/merge

SELECTED_JARS += fastcgi4j_api
trees_fastcgi4j_api += api

SELECTED_JARS += fastcgi4j_app
trees_fastcgi4j_app += app

SELECTED_JARS += fastcgi4j_engine
trees_fastcgi4j_engine += engine

SELECTED_JARS += fastcgi4j_proto
trees_fastcgi4j_proto += proto

SELECTED_JARS += fastcgi4j_inet
trees_fastcgi4j_inet += inet

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
SELECTED_JARS += fastcgi4j_unix
endif

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_JUNIXSOCKET))),)
SELECTED_JARS += fastcgi4j_junixsocket
endif

trees_fastcgi4j_unix += unix
trees_fastcgi4j_junixsocket += junixsocket

jars += $(SELECTED_JARS)

jars += fastcgi4j_demos
trees_fastcgi4j_demos += demos

jars += tests

roots_api += $(found_api)
roots_app += $(found_app)
deps_app += api
roots_engine += $(found_engine)
deps_engine += api
deps_engine += app
deps_engine += proto
roots_proto += $(found_proto)
roots_unix += $(found_unix)
deps_unix += app
deps_unix += proto
roots_junixsocket += $(found_junixsocket)
deps_junixsocket += app
deps_junixsocket += proto
roots_inet += $(found_inet)
deps_inet += app
deps_inet += proto
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
DOC_CORE=fastcgi4j$(DOC_CORE_SFX)
DOC_PKGS += uk.ac.lancs.fastcgi
DOC_PKGS += uk.ac.lancs.fastcgi.context
DOC_PKGS += uk.ac.lancs.fastcgi.app
DOC_PKGS += uk.ac.lancs.fastcgi.engine
DOC_PKGS += uk.ac.lancs.fastcgi.engine.util
DOC_PKGS += uk.ac.lancs.fastcgi.engine.std
DOC_PKGS += uk.ac.lancs.fastcgi.proto
DOC_PKGS += uk.ac.lancs.fastcgi.proto.serial
DOC_PKGS += uk.ac.lancs.fastcgi.transport
DOC_PKGS += uk.ac.lancs.fastcgi.transport.inet
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_JUNIXSOCKET))),)
DOC_PKGS += uk.ac.lancs.fastcgi.transport.junixsocket
endif
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
DOC_PKGS += uk.ac.lancs.fastcgi.transport.native_unix
endif

$(BINODEPS_OBJDIR)/native.lo: | tmp/tree-unix.compiled

all:: installed-jars installed-libraries
installed-jars:: $(SELECTED_JARS:%=out/%.jar)
installed-jars:: $(SELECTED_JARS:%=out/%-src.zip)

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install:: install-jars install-hidden-libraries install-scripts

tidy::
	@$(PRINTF) 'Deleting trash\n'
	@$(FIND) . -name "*~" -delete

clean:: tidy

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
	$(SED) -i 's/Copyright (c)\s[-0-9,]\+\Lancaster University/Copyright (c) $(YEARS), Lancaster University/g'
