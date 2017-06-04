# gitbucket-ipynb-plugin

A GitBucket plugin for rendering `.ipynb` file. `.ipynb` is [Jupyter](http://jupyter.org/) or [IPython](https://ipython.org/) file format.

## screenshot

![20170604-021541 - mediatest sample ipynb at master - root mediatest - google chrome](https://cloud.githubusercontent.com/assets/6997928/26756112/96224284-48d6-11e7-894d-3bed93a2674d.png)

## Install

1. Download *.jar from Releases.
2. Deploy it to `GITBUCKET_HOME/plugins`.
3. Restart GitBucket.

## Usage

Store `.ipynb` file (from File->Download as->Notebook) to your Git repository and push it to GitBucket.
If you don't know using IPython/Jupyter, try google it!

## Limitations

- Javascript output is disabled.
- traceback is not correctly rendered(not supported ANSI escape sequences and tabs.)
- only support version 3.0/4.0 ipynb files.
- only tested Python2 kernel.

## BugReport

If your `.ipynb` is not correctly displayed, please make an issue with your `.ipynb` file if you can.

