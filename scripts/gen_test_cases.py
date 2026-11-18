#!/usr/bin/env python3
"""生成 6 道示例题的隐藏测试点（固定随机种子，结果可复现）。

用法：
    python scripts/gen_test_cases.py

产物：
    1. docs/sql/test_cases.sql   —— 重置 6 道题的测试点，可直接导入现有库
    2. docs/sql/schema.sql       —— 自动替换其中的 test_case 插入语句

设计原则：
    - 每个题 6 个测试点，总分 100（17/17/17/17/16/16）
    - 公开样例不参与评分，所有评分点均不与题面样例重复
    - 每个题至少包含：边界值、负数/极值、一组大数据
    - 随机数据由固定 seed 生成，任何人重跑结果一致
"""

import random
import re
from pathlib import Path

SEED = 20261118
TITLES = [
    "A+B Problem",
    "数列求和",
    "求最大值",
    "回文串判断",
    "斐波那契数列",
    "排序去重",
]
SCORES = [17, 17, 17, 17, 16, 16]

ROOT = Path(__file__).resolve().parents[1]
SCHEMA_PATH = ROOT / "docs" / "sql" / "schema.sql"
RESET_PATH = ROOT / "docs" / "sql" / "test_cases.sql"


def esc(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n")


def numeric_case(n, values):
    payload = f"{n}\n" + " ".join(str(v) for v in values) + "\n"
    return payload


def build_cases():
    rnd = random.Random(SEED)

    add_cases = [
        ("0 0\n", "0\n"),
        ("-1 -2\n", "-3\n"),
        ("1000000000 1000000000\n", "2000000000\n"),
        ("-1000000000 -1000000000\n", "-2000000000\n"),
        (
            f"{rnd.randint(-10**9, 10**9)} {rnd.randint(-10**9, 10**9)}\n",
            None,
        ),
        (f"{rnd.randint(0, 10**9)} 0\n", None),
    ]
    resolved_add = []
    for payload, expected in add_cases:
        if expected is None:
            a, b = map(int, payload.split())
            expected = f"{a + b}\n"
        resolved_add.append((payload, expected))

    sum_values = [
        [-(10**9)],
        [0, 0],
        [-1, -2, -3, -4, -5],
        [rnd.randint(-(10**9), 10**9) for _ in range(10000)],
        [rnd.randint(-1000, 1000) for _ in range(3)],
        [rnd.randint(-100, 100) for _ in range(7)],
    ]
    sum_cases = [
        (numeric_case(len(values), values), f"{sum(values)}\n")
        for values in sum_values
    ]

    max_values = [
        [-(10**9)],
        [-(10**9), -1],
        [-9, -3, -7, -1, -5],
        [rnd.randint(-(10**9), 10**9) for _ in range(10000)],
        [0, 0, 0],
        [rnd.randint(-100, 100) for _ in range(4)],
    ]
    max_cases = [
        (numeric_case(len(values), values), f"{max(values)}\n")
        for values in max_values
    ]

    base = "".join(rnd.choice("abcdefghijklmnopqrstuvwxyz") for _ in range(10))
    palindrome = base + base[::-1]
    long_palindrome = "a" * 499 + "b" + "a" * 499
    clearly_not_palindrome = "z" + base + "q"
    pal_cases = [
        ("a\n", "YES\n"),
        ("ab\n", "NO\n"),
        ("abcba\n", "YES\n"),
        (f"{palindrome}\n", "YES\n"),
        (f"{clearly_not_palindrome}\n", "NO\n"),
        (f"{long_palindrome}\n", "YES\n"),
    ]

    def fib(n):
        a, b = 1, 1
        for _ in range(n - 1):
            a, b = b, a + b
        return a

    fib_cases = [(f"{n}\n", f"{fib(n)}\n") for n in (1, 2, 3, 20, 40, 50)]

    sort_values = [
        [0],
        [-5, -5],
        [7, 7, 7, 7, 7],
        [rnd.randint(-(10**9), 10**9) for _ in range(1000)],
        [rnd.randint(-100, 100) for _ in range(6)],
        [-(10**9), 0, 10**9],
    ]
    sort_cases = [
        (
            numeric_case(len(values), values),
            " ".join(str(v) for v in sorted(set(values))) + "\n",
        )
        for values in sort_values
    ]

    return {
        "A+B Problem": resolved_add,
        "数列求和": sum_cases,
        "求最大值": max_cases,
        "回文串判断": pal_cases,
        "斐波那契数列": fib_cases,
        "排序去重": sort_cases,
    }


def insert_statements(cases_by_title):
    blocks = []
    for title in TITLES:
        for order_no, (payload, expected) in enumerate(cases_by_title[title], start=1):
            score = SCORES[order_no - 1]
            blocks.append(
                "INSERT INTO `test_case` (`problem_id`, `order_no`, `input`, `expected_output`, `score`)\n"
                f"SELECT p.`id`, {order_no}, '{esc(payload)}', '{esc(expected)}', {score} FROM `problem` p\n"
                f"WHERE p.`title` = '{title}'\n"
                "  AND NOT EXISTS (SELECT 1 FROM `test_case` t "
                f"WHERE t.`problem_id` = p.`id` AND t.`order_no` = {order_no});"
            )
    return "\n\n".join(blocks)


def reset_sql(cases_by_title, insert_block):
    titles = ", ".join(f"'{title}'" for title in TITLES)
    return (
        "-- 由 scripts/gen_test_cases.py 生成（seed=20261118），请勿手工修改\n"
        "-- 用途：重置 6 道示例题的隐藏测试点；公开样例不参与评分\n"
        "USE `oj`;\n\n"
        "DELETE FROM `test_case`\n"
        f"WHERE `problem_id` IN (SELECT `id` FROM `problem` WHERE `title` IN ({titles}));\n\n"
        f"{insert_block}\n"
    )


def update_schema(insert_block):
    text = SCHEMA_PATH.read_text(encoding="utf-8")
    text = re.sub(r"INSERT INTO `test_case`[\s\S]*?;\n", "", text)
    anchor = re.search(
        r"INSERT INTO `problem`[\s\S]*?'排序去重'[\s\S]*?;\n", text
    )
    if not anchor:
        raise SystemExit("未找到「排序去重」的题目插入语句，请检查 schema.sql")
    block = (
        "\n\n-- ------------------------------------------------------------\n"
        "-- 隐藏测试点（由 scripts/gen_test_cases.py 生成，样例不参与评分）\n"
        "-- ------------------------------------------------------------\n"
        f"{insert_block}\n"
    )
    SCHEMA_PATH.write_text(
        text[: anchor.end()] + block + text[anchor.end() :], encoding="utf-8"
    )


def main():
    cases_by_title = build_cases()
    total = sum(len(items) for items in cases_by_title.values())
    insert_block = insert_statements(cases_by_title)

    RESET_PATH.write_text(
        reset_sql(cases_by_title, insert_block), encoding="utf-8"
    )
    update_schema(insert_block)

    print(f"生成完成：{len(TITLES)} 道题 / {total} 个测试点")
    for title in TITLES:
        points = cases_by_title[title]
        print(f"  {title}: {len(points)} 点，总分 {sum(SCORES[: len(points)])}")
    print(f"  -> {RESET_PATH.relative_to(ROOT)}")
    print(f"  -> {SCHEMA_PATH.relative_to(ROOT)}")


if __name__ == "__main__":
    main()