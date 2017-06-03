# gitbucket-ipynb-plugin

A GitBucket plugin for rendering `.ipynb` file. `.ipynb` is [Jupyter](http://jupyter.org/) or [IPython](https://ipython.org/) file format.

## screenshot

![20170604-021541 - mediatest sample ipynb at master - root mediatest - google chrome](https://cloud.githubusercontent.com/assets/6997928/26756112/96224284-48d6-11e7-894d-3bed93a2674d.png)

## Install

1. Download *.jar from Releases.
2. Deploy it to `GITBUCKET_HOME/plugins`.
3. Restart GitBucket.

## Limitations

- Javascript output is disabled.
- traceback is not correctly rendered(not supported ANSI escape sequences and tabs.)
- only support version 4.0 ipynb files.