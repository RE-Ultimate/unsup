> [!caution]
> This build is liable to be ***HIGHLY UNSTABLE***. Use with care! See the notes on v1.3.0-experimental1.

* French and Korean translation updates (thanks, Erisatis and Skye!)
* Complete German translation (thanks, SammyKoch!)
* Partial Dutch translation (thanks, the Roselaces!)
* Fix double-escaping with GitHub raw URL autofix
* Completely rewrote the translations system
  * Plural rules support
  * Now works around MultiMC/Prism Launcher `-Duser.language=en` garbage
* Don't retry failed DNS or initial connections for the initial pack manifest or puppet assets
* Retry downloading puppet assets from exa.y2k.diy (a non-CDN domain) to possibly make it work in China
  * They seem to aggressively block any and all CDN hosts, but our normal server is fine
