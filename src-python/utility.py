import os
import sys

def getscrpath():
    return os.path.dirname(os.path.realpath(sys.argv[0]))

def patthatcat(*purr):
    return os.path.join(*purr) # :3 cute catto