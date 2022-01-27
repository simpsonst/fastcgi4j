all::

PREFIX=/usr/local

-include fastcgi4j-env.mk
-include $(subst $(jardeps_space),\$(jardeps_space),$(CURDIR))/config.mk

JARDEPS_SRCDIR=src/java
JARDEPS_DEPDIR=src
JARDEPS_MERGEDIR=src/merge

SELECTED_JARS += fastcgi4j_api
trees_fastcgi4j_api += api

SELECTED_JARS += fastcgi4j_app
trees_fastcgi4j_app += app

SELECTED_JARS += fastcgi4j_conn
trees_fastcgi4j_conn += conn

SELECTED_JARS += fastcgi4j_conn
trees_fastcgi4j_engine += engine

SELECTED_JARS += fastcgi4j_impl
trees_fastcgi4j_impl += impl

SELECTED_JARS += fastcgi4j_proto
trees_fastcgi4j_proto += proto

SELECTED_JARS += fastcgi4j_unix
trees_fastcgi4j_unix += unix

jars += $(SELECTED_JARS)


include jardeps.mk
-include jardeps-install.mk

DOC_OVERVIEW=src/java-overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(SELECTED_JARS))
DOC_CORE=fastcgi4j$(DOC_CORE_SFX)

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
