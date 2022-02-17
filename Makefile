release:
	mvn clean deploy -P release
	#mvn release:clean release:prepare &&  mvn release:perform

prepare:
	mvn release:clean release:prepare
