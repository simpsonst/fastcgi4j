all::

PREFIX=/usr/local

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

ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
SELECTED_JARS += fastcgi4j_unix
trees_fastcgi4j_unix += unix
endif

jars += $(SELECTED_JARS)

jars += fastcgi4j_demos
trees_fastcgi4j_demos += demos

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
roots_demos += $(found_demos)
deps_demos += api
deps_demos += app

include jardeps.mk
-include jardeps-install.mk

DOC_OVERVIEW=src/java/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(SELECTED_JARS))
DOC_CORE=fastcgi4j$(DOC_CORE_SFX)
DOC_PKGS += uk.ac.lancs.fastcgi
DOC_PKGS += uk.ac.lancs.fastcgi.engine
DOC_PKGS += uk.ac.lancs.fastcgi.engine.util
DOC_PKGS += uk.ac.lancs.fastcgi.engine.std
DOC_PKGS += uk.ac.lancs.fastcgi.proto
DOC_PKGS += uk.ac.lancs.fastcgi.proto.ap
DOC_PKGS += uk.ac.lancs.fastcgi.conn
ifneq ($(filter true t y yes on 1,$(call lc,$(ENABLE_UNIX))),)
DOC_PKGS += uk.ac.lancs.fastcgi.conn.unix
endif

all:: installed-jars
installed-jars:: $(SELECTED_JARS:%=out/%.jar)
installed-jars:: $(SELECTED_JARS:%=out/%-src.zip)

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install:: install-jars

tidy::
	@$(PRINTF) 'Deleting trash\n'
	@$(FIND) . -name "*~" -delete

clean:: tidy
