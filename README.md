# gitbucket-ipynb-plugin

A GitBucket plugin for rendering `.ipynb` file.

## Download

You can download binary from Release page.

## Installation

Downloaded binary to `$GITBUCKET_HOME/plugins` directory.

## Limitations

- Javascript output is disabled.
- traceback is not correctly rendered(not supported ANSI escape sequences and tabs.)
- only support version 4.0 ipynb files.