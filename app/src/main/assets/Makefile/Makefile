O := build
CFLAGS := -g -Wall -lm

all: main
main: main.cpp
	$(CXX) $^ -o $(O)/$@ $(CFLAGS)

.PHONY: clean
clean:
	rm -rf $(O)/*