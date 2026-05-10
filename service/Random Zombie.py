import random
from collections import deque

# =========================
# INPUT MAP (dán map của bạn vào đây)
# =========================
MAP = 

ROWS = len(MAP)
COLS = len(MAP[0])

# =========================
# Tìm start (2) và end (3)
# =========================
def find_points():
    start = end = None
    for r in range(ROWS):
        for c in range(COLS):
            if MAP[r][c] == 2:
                start = (r, c)
            elif MAP[r][c] == 3:
                end = (r, c)
    return start, end


# =========================
# BFS tìm đường đi chính
# =========================
def find_path(start, end):
    if not start or not end:
        return set()

    queue = deque([start])
    visited = set([start])
    parent = {}

    directions = [(1,0), (-1,0), (0,1), (0,-1)]

    while queue:
        r, c = queue.popleft()

        if (r, c) == end:
            path = []
            cur = end
            while cur != start:
                path.append(cur)
                cur = parent[cur]
            path.append(start)
            path.reverse()
            return set(path)

        for dr, dc in directions:
            nr, nc = r + dr, c + dc

            if (0 <= nr < ROWS and 0 <= nc < COLS and
                MAP[nr][nc] != 1 and (nr, nc) not in visited):

                visited.add((nr, nc))
                parent[(nr, nc)] = (r, c)
                queue.append((nr, nc))

    return set() 


# =========================
# Khoảng cách Manhattan
# =========================
def manhattan(a, b):
    return abs(a[0] - b[0]) + abs(a[1] - b[1])


# =========================
# Khoảng cách tới main path
# =========================
def distance_to_path(pos, path):
    if not path:  
        return float('inf')
    return min(manhattan(pos, p) for p in path)


# =========================
# Random zombie position
# =========================
def get_random_zombie_position(min_dist_player=8, min_dist_path=3):
    start, end = find_points()

    main_path = find_path(start, end)

    candidates = []

    for r in range(ROWS):
        for c in range(COLS):
            if MAP[r][c] != 0:
                continue

            pos = (r, c)

            # tránh đường chính
            if pos in main_path:
                continue

            # tránh gần player
            if manhattan(pos, start) < min_dist_player:
                continue

            # tránh gần path
            if distance_to_path(pos, main_path) < min_dist_path:
                continue

            candidates.append(pos)

    # =========================
    # FALLBACK (nếu quá ít vị trí)
    # =========================
    if not candidates:
        for r in range(ROWS):
            for c in range(COLS):
                if MAP[r][c] == 0:
                    candidates.append((r, c))

    

    return random.choice(candidates)


# =========================
# MAIN TEST
# =========================
if __name__ == "__main__":
    start, end = find_points()

    path = find_path(start, end)

    zombie_pos = get_random_zombie_position()
    print(zombie_pos)