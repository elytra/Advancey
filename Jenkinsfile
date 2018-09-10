node {
	checkout scm
	sh 'git submodule update --init'
	sh './gradlew setupCiWorkspace clean build'
	archive 'build/libs/*jar'
}
