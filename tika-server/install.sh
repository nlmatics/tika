mvn -fn -U clean install:install-file \
   -Dfile=/home/yi_zhang/nlm/tika/tika-parsers/target3/tika-parsers/tika-parsers/2.0.5/tika-parsers-2.0.5.jar \
   -DgroupId=tika-parsers \
   -DartifactId=tika-parsers \
   -Dversion=2.0.5 \
   -Dpackaging=jar \
   -DgeneratePom=true
