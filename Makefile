release:
	mvn release:clean release:prepare &&  mvn release:perform
	#mvn clean deploy -P release

prepare:
	mvn release:clean release:prepare
