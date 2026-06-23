TEX := pdflatex
TEXFLAGS := -interaction=nonstopmode -halt-on-error -file-line-error
MAIN := report.tex
JOB := report
SOURCES := $(MAIN) $(wildcard sections/*.tex)

.PHONY: all clean

all: $(JOB).pdf

$(JOB).pdf: $(SOURCES)
	$(TEX) $(TEXFLAGS) -jobname=$(JOB) $(MAIN)
	$(TEX) $(TEXFLAGS) -jobname=$(JOB) $(MAIN)

clean:
	rm -f $(JOB).pdf \
	      $(JOB).aux \
	      $(JOB).log \
	      $(JOB).out \
	      $(JOB).toc \
	      $(JOB).lof \
	      $(JOB).lot \
	      $(JOB).fls \
	      $(JOB).fdb_latexmk \
	      $(JOB).synctex.gz \
	      $(JOB).bbl \
	      $(JOB).blg \
	      $(JOB).bcf \
	      $(JOB).run.xml
