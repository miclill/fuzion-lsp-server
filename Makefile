SOURCEDIR = src
JAVA_FILES = $(shell find $(SOURCEDIR) -name '*.java')

JARS_FOR_CLASSPATH = jars/org.eclipse.lsp4j-0.12.0.jar:jars/org.eclipse.lsp4j.generator-0.12.0.jar:jars/org.eclipse.lsp4j.jsonrpc-0.12.0.jar:jars/gson-2.8.7.jar
JARS = $(subst :, ,$(JARS_FOR_CLASSPATH))

all: classes
	java -cp classes:build/classes:$(JARS_FOR_CLASSPATH) Main -tcp

classes: $(JAVA_FILES) $(JARS) build_fuzion
	mkdir -p $@
	javac -classpath $(JARS_FOR_CLASSPATH):build/classes -d $@ $(JAVA_FILES)

stdio: classes
	java -cp classes:build/classes:$(JARS_FOR_CLASSPATH) Main

debug: classes
	mkdir -p runDir
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000 -cp classes:build/classes:$(JARS_FOR_CLASSPATH) Main -tcp

jars/org.eclipse.lsp4j-0.12.0.jar:
	mkdir -p $(@D)
	wget -O $@ https://repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j/0.12.0/$(@F)

jars/org.eclipse.lsp4j.generator-0.12.0.jar:
	mkdir -p $(@D)
	wget -O $@ https://repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.generator/0.12.0/$(@F)

jars/org.eclipse.lsp4j.jsonrpc-0.12.0.jar:
	mkdir -p $(@D)
	wget -O $@ https://repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.jsonrpc/0.12.0/$(@F)

jars/gson-2.8.7.jar:
	mkdir -p $(@D)
	wget -O $@ https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.7/$(@F)

build_fuzion:
	make -f fuzion/Makefile