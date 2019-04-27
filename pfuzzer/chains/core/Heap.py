# coding=utf-8
"""
Contains code that creates and uses a heap.
"""

import heapq
from typing import List


class Heap:
    """
    Wraps the heapq class.
    """
    heap: List = []

    def __init__(self):
        self.heap = []

    def __len__(self):
        return len(self.heap)

    def __iter__(self):
        return self.heap.__iter__()

    def push(self, item):
        """
        Pushes the item on the heap and keeps the heap invariant.
        :param item:
        """
        heapq.heappush(self.heap, item)

    def pop(self):
        """
        Returns the smallest element from the heap.
        :return:
        """
        return heapq.heappop(self.heap)

    def clear(self):
        """
        Empties the heap.
        """
        self.heap = []

    def empty(self):
        """
        Checks if heap is empty.
        :return: True if empty, else false.
        """
        return self.heap == []

    def __str__(self):
        return str(self.heap)

    def __repr__(self):
        return repr(self.heap)
