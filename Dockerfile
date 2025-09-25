FROM bellsoft/liberica-runtime-container:jre-25-slim-musl
#Use Bellsoft's liberica runtime container with JRE 25 built on the lightweight Alpaquita Linux
#Don't need the full JDK, JRE by itself will suffice as we'll use a JAR (Java Archive) containing the compiled files. (Compiled Java files work in any environment using the Java Virtual Machine contained in the JRE)

LABEL Maintainer="Adam Rebes <https://github.com/Adzzzy>"

#copy the JAR file from the host machine into the image's present working directory with COPY BasicPaxos.jar . (Image's working directory is root (/) by default)
COPY BasicPaxos.jar .
#.dockerignore file not necessary as we've specified a specific file to copy over

#Entrypoint commands given in Exec form i.e. ["executable", "param1", "param2"]
ENTRYPOINT [ "java", "-jar", "BasicPaxos.jar" ]
#java command will run the BasicPaxos Java Archive. Will start the program by running Client.class as specified in the JAR's manifest.