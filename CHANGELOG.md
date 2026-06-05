> [!caution]
> This build is liable to be ***HIGHLY UNSTABLE***. Use with care!

Changes compared to the latest 1.2 snapshot:

* **Removed OkHttp in favor of a backport of the HttpClient from Java 25 (as introduced in Java 11)**
  * Release-blocking limitations:
    * Uses *only* CRLs for certificate revocation checks, OCSP stapling is ignored
    * Insecure hosts have not been ported
    * ~~Authorizers have not been ported~~ Now ported!
    * DNS-over-HTTPS has not been ported (but we might drop it, does anyone even use it?)
  * This will certainly break things, but we're stuck on an old version of OkHttp to retain Java 8 support (also it is a very frustrating dependency due to being written in Kotlin and not being designed to be cleaned up — unsup needs to not leave behind garbage threads when the main program starts after the agent completes!)
  * This removes the Kotlin stdlib from unsup and makes the jar smaller, despite pulling in a massive chunk of Java code
  * *Please test this!*
* Completely rewrote the translations system
  * Plural rules support
  * Now works around MultiMC/Prism Launcher `-Duser.language=en` garbage
* Don't retry failed DNS or initial connections for the initial pack manifest or puppet assets
* Retry downloading puppet assets from exa.y2k.diy (a non-CDN domain) to possibly make it work in China
  * They seem to aggressively block any and all CDN hosts, but our normal server is fine
