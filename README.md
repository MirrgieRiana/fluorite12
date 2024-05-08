# NAME

fluorite12(flc) - Interpreted language for one-liners

# SYNOPSIS

```shell
$ flc ' "Hello, World" '
# Hello, World

$ flc '[1 .. 3 | x => [1 .. 3 | x * _]]'
# [[1;2;3];[2;4;6];[3;6;9]]

$ seq 1 3 | flc 'IN | +_ * 10'
# 10
# 20
# 30

$ echo '{"a": [10, {"b": 30}, 20]}' | flc 'IN | _.$*.a.1.b'
# 30

$ flc 'f := n -> n <= 0 ? 1 : n * f(n - 1); f(5)'
# 120

$ flc '(f -> f(f))(f -> n -> n <= 0 ? 1 : n * f(f)(n - 1))(5)'
# 120
```

# DESCRIPTION

fluorite12 is an interpreter language designed for one-liner scripts.
It aims to allow flexibility and functionality in a minimal amount of code
with most basic features accessible through operators.
Its command-line interface is optimized to enable writing executable programs with minimal code.
For example, the command `$ flc ' "Hello, World" '` will display `Hello, World`.

# INSTALL

Linux:

```shell
curl -s https://raw.githubusercontent.com/MirrgieRiana/fluorite12/release/install.sh | sudo bash
```

Download the `flc` binary to the `./fluorite12`, and register it in `/usr/local/bin`.

# SYNTAX

- [Japanese](SYNTAX-ja.md)