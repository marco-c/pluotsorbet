.PHONY: all test tests java certs clean

all: java tests

test: all
	rm -f test.log
	killall python Python || true
	python -m SimpleHTTPServer &
	python tests/echoServer.py &
	casperjs --engine=slimerjs test `pwd`/tests/automation.js > test.log
	killall python Python || true
	python dumplog.py
	if grep -q FAIL test.log; \
	then false; \
	else true; \
	fi

tests:
	make -C tests

java:
	make -C java

certs:
	make -C certs

clean:
	rm -f j2me.js `find . -name "*~"`
	make -C tests clean
	make -C java clean
