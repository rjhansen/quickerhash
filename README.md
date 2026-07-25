# QuickerHash

[![forthebadge](https://ForTheBadge.com/images/badges/made-with-java.svg)](https://forthebadge.com)

## Why make it?
A friend of mine teaches digital security to journalists, human rights
workers, and other such people who really need to be aware of digital 
security.

Unfortunately, we in the digital security developer community have
sort of failed them. We’ve done great work building tools for _us,_ but
not so much tools for _them._ Or, as XKCD has it,

[![xkcd](https://imgs.xkcd.com/comics/average_familiarity.png)](https://xkcd.com/2501/)

You might think that `sha256sum` is easy enough to use, but the reality
is as soon as you expect non-technical users to open a terminal window
you’ve lost at least half your audience.

At present, my friend is stuck using Ted Smith’s [QuickHash-GUI](https://www.quickhash-gui.org/)
in his digital security classes. On the plus side, it achieves the
objective of letting nontechnical users compute hashes without ever
needing to interact with a terminal. On the minus side, it’s not
exactly easy to use itself: the user interface is pretty clunky.

We can do better.

So, with a respectful nod of the head to Ted Smith and thanks for
his work, here’s QuickerHash.

## Features

* Crossplatform (tested on macOS, Windows, and Fedora Linux)
* Prebuilt binaries (macOS/Apple Silicon, Windows/x86_64, Linux x86_64)
* macOS binaries are properly signed and notarized by Apple
* 100% libre software (Apache 2.0 licensed)
* Supports
    * MD2
    * MD5
    * SHA-1
    * SHA-224
    * SHA-256
    * SHA-384
    * SHA-512
    * SHA-512/224
    * SHA-512/226
    * SHA3-224
    * SHA3-256
    * SHA3-384
    * SHA3-512
    * SHAKE128-256
    * SHAKE256-512
* Good look-and-feel: it uses FlatLAF, which is the basis for the
  look-and-feel used in JetBrains’ Intellij IDEA.
* Written in 100% Java. I’d prefer writing it in Rust, myself, but
  there are a lot more Java hackers than Rust ones, and that’s good
  for its long-term viability.
* Self-contained executable: you don't need a Java virtual machine,
  as the executables run in their own Java environment. (Yes, this
  _does_ make it a 32MiB download. It’s 2026; get over it.)
* A user interface designed by someone who’s formally studied
  user interface design.

## Using pre-built binaries

### macOS

Pre-built binaries exist for Macs running Apple Silicon. They're
properly code signed and notarized by Apple. They install just like
any other application contained in a disk image.

### Linux

The RPM I provide here is signed by my GnuPG certificate. I'd suggest:

```bash
$ gpg --keyserver hkps://keys.openpgp.org \
    --recv-key CC11BE7CBBED77B120F37B011DCBDC01B44427C7
$ gpg --armor --export CC11BE7CBBED77B120F37B011DCBDC01B44427C7 > key.asc
$ sudo rpm --import key.asc 
```

This will allow you to ensure the integrity of your download.

### Windows

Unfortunately, I don't have the disposable income to afford a Windows
code signing certificate — sorry. You can, however, check the detached GnuPG
signature for the package.
