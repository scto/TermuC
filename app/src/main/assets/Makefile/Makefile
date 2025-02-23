CFLAGS := -g -Wall
LFLAGS := -lm

all: main
$(O)/%: $(O)/%.o
	$(CXX) $^ -o $@ $(LFLAGS)

$(O)/%.o: %.cpp
	$(CXX) -c $^ -o $@ $(CFLAGS)

.PHONY: clean
clean:
	rm -rf $(O)/*
