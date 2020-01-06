# build file for PAM Auth

all:	compile

.PHONY:	compile
compile:
	mvn compile

.PHONY:	test
test:
	mvn test

.PHONY:	package
package:
	mvn package

.PHONY:	install
install:
	mvn install

.PHONY:	site
site:
	mvn site

.PHONY:	prepare-release
prepare-release:
	mvn release:prepare -e

.PHONY:	perform-release
perform-release:
	mvn release:perform -e

.PHONY:	run
run:
	mvn compile exec:exec

.PHONY:	docs
docs:
	mvn javadoc:javadoc

.PHONY:	dist
dist:
	mvn package appassembler:assemble

.PHONY:	clean
clean:
	mvn clean
