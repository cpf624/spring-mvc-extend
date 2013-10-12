install:
	mvn install -Dmaven.test.skip=true
push:
	git push origin master
package:clean
	mvn package -Dmaven.test.skip=true
clean:
	mvn clean
deploy:
	mvn deploy -Dmaven.test.skip=true
update:
	git pull
