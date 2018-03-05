#! /bin/bash

pushd ..
	mvn test
popd
diff -u ../src/test/resources/ToodleTest-ref.txt ../target/test-classes/ToodleTest-last.txt --color=auto
