mvn -fn -U clean install:install-file \
   -Dfile=/Users/reshavabraham/work/nlm-tika/tika/tika-parsers/target3/tika-parsers/tika-parsers/2.0.4/tika-parsers-2.0.4.jar \
   -DgroupId=tika-parsers \
   -DartifactId=tika-parsers \
   -Dversion=2.0.4 \
   -Dpackaging=jar \
   -DgeneratePom=true
