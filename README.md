# The perfect web server

Version 1.0.2

## Goal

> Write programs that do one thing and do it well. \
― Unix philosophy

The goal of this project is to create a library that allows you to implement a simple web server
while writing as little code as possible.

## The Maven dependency

To link the library to your Maven project, add the following dependency to `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.kniazkov</groupId>
        <artifactId>webserver</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

## How to use

Implement your custom request handler.
The `Handler` interface as well as other classes are provided with detailed JavaDoc information.

```java
import com.kniazkov.webserver.*;

class MyHandler implements Handler {
    @Override
    public Response handle(final Request request) {
        //...
    }
}
```

Specify options for starting the server. In the simplest case, the default options are good.

If necessary, create a folder from which to read your project's static files, such as `index.html`.  
The default name of this folder is `www`.

Then, start the web server.

```java
import com.kniazkov.webserver.*;

class MyProject {
    public static void main(String[] args) {
        Handler handler = new MyHandler();
        Options options = new Options();
        //...
        Server.start(options, handler);
    }
}
```
That's all. Also, you can find some examples in the `example` subfolder.  
If you have any questions, please create a ticket in the project.
