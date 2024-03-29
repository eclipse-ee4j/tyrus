[![Build Status](https://travis-ci.org/eclipse-ee4j/tyrus.svg?branch=master)](https://travis-ci.org/eclipse-ee4j/tyrus)

# Eclipse Tyrus

Eclipse Tyrus is the open source
<a href="https://projects.eclipse.org/projects/ee4j.websocket">JSR 356 - Java API for WebSocket</a>
reference implementation
for easy development of WebSocket applications.Eclipse Tyrus is also 
a Jakarta WebSocket 2.0 compatible implementation.

 WebSocket protocol defined by IETF 
provides bidirectional communication between the server and the remote host. The
pros are mainly the ability to communicate both ways, low latency and small
communication overhead. Therefore, Tyrus and WebSocket in general are suitable for web
applications that require sending a huge volume of relatively small messages like
online games or market ticker broadcasting.

## Building Eclipse Tyrus

Building Tyrus can be done using `mvn clean install`, but sometimes (such as for building 2.x from a tag) 
`mvn clean install -Pstaging` would be required.

## Tyrus Git Branches

| branch | Jakarta Version                 | Tyrus Version |
|--------|---------------------------------|---------------|
| master | Java EE 8 / Jakarta EE 8 branch | Tyrus 1.x     |
| 2.0.x  | Jakarta EE 9 branch             | Tyrus 2.0.x   |
| 2.1.x  | Jakarta EE 10 branch            | Tyrus 2.1.x   |
| 2.x    | Jakarta EE 11 branch            | Tyrus 2.2.x   |

## Licensing

- [Eclipse Public License 2.0](https://projects.eclipse.org/license/epl-2.0)
- [GNU General Public License, version 2 with the GNU Classpath Exception](https://projects.eclipse.org/license/secondary-gpl-2.0-cp)

## Links

- Documentation: https://eclipse-ee4j.github.io/tyrus/
- Website: https://projects.eclipse.org/projects/ee4j.tyrus
- Issues tracker: https://github.com/eclipse-ee4j/tyrus/issues
- Mailing list: https://accounts.eclipse.org/mailing-list/tyrus-dev 

HTML 5 logo by W3C - http://www.w3.org
