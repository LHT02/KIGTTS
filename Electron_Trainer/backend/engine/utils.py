import shutil
from typing import List, Optional


def find_executable(names: List[str]) -> Optional[str]:
    for name in names:
        if shutil.which(name):
            return name
    return None

