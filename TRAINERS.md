# README for Trainers

-----

This is not a user manual. This is just some information from QuickerHash’s author
(that is to say me, Rob).

## Why use QuickerHash in digital security training?

* Because it's a simple enough tool to not frighten non-technical users, while still 
  capable enough to be useful in a wide variety of situations.
* Because it's crossplatform with identical look and feel across macOS, Windows, and
  Linux. Whatever skills and drills you teach in the lab, they will be able to take
  home with them even if home is running a different OS entirely.
* Because it's reliable.
* Because it's open-source software (or libre software or free software, as you like).
  Your students can legally share it, and the skills you teach them, with anyone they
  like.
* Because it's actively maintained.

## Sensible defaults, out of the box.

The most commonly used hashes are MD5, SHA-1, and SHA-256. The first two are of course
obsolete, but there are still a lot of things on the web with provenance assured by
them. By default, out of the box, your student will only be able to choose from the three 
most commonly used hashes. It's great when we can reduce cognitive spam on newbies.

## But adaptable to student needs.

If one of your students needs to verify a document from the Ukrainian government,
that could be a problem since the Ukrainian government uses their own unique hash algorithm.

Could. Until your student click "Hashes" and "Ukrainian", at which point the DSTU7564 hashes
innocuously appear as selectable options wherever the student needs to choose a hash.

Out of the box QuickerHash supports the national standards of the U.S., Ukraine,
Russia, and China.

## Batteries included. Very included.

If students have an exotic hash they need to use, just click "Hashes" and "Exotics".
QuickerHash uses the BouncyCastle cryptography library, and all of its sixty-two
hashes.

