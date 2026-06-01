* Fix Puppet getting stuck or crashing after a window is closed
* Log things that crash the Puppet main thread
* Treat `side = ""` in Packwiz metafiles as a synonym for `both`, seen in Prism Launcher files
* Fix `<ul>` elements in the OpenGL puppet's description display overflowing the window bounds
* Fix `<br>` not being permitted in HTML
* Fixed the Simplified Chinese language code (`zh-Hans` → `zh-CN`)
* Fixed TOML files not being loaded as UTF-8
* Added support for `sigsum`-signed manifests (thanks, skyevg! [#67](https://git.sleeping.town/exa/unsup/pulls/67))
* Now uses our Toolbox library internally
* Which key validated a signature is now printed
* **Removed OkHttp in favor of a Java HttpClient backport**
  * Currently missing features, which will be added before a proper release:
    * Insecure hosts
    * Authorizers
    * DNS-over-HTTPS (might get dropped)
  * This will certainly break things, but we're stuck on an old version of OkHttp to avoid LLM slop and it is a very frustrating dependency due to being written in Kotlin and not being designed to be cleaned up
  * This removes the Kotlin stdlib from unsup and makes the jar smaller, despite pulling in a massive chunk of Java code
  * *Please test this!*
