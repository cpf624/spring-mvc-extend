install:
	mvn install -Dmaven.test.skip=true
push:
	git push origin master
package:clean
	mvn package -Dmaven.test.skip=true
clean:
	mvn clean
deploy:
	mvn javadoc:jar source:jar deploy -Dmaven.test.skip=true
update:
	git pull
