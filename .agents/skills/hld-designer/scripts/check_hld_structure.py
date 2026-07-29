#!/usr/bin/env python3
"""
HLD 结构校验脚本。
退出码 = 0 表示合规,非 0 表示有违规。

Usage:
    python3 check_hld_structure.py <file1.md> [file2.md ...]
"""
import sys
import re
import os
from pathlib import Path

REQUIRED_HEADER_FIELDS = [
    "**项目名称**",
    "**文档类型**",
    "**创建日期**",
    "**创建人**",
    "**审核人**",
    "**版本号**",
    "**状态**",
]

REQUIRED_SECTIONS = [
    ("## 1. 概述", "概述章节缺失"),
    ("## 2. 处理流程图", "处理流程图章节缺失"),
    ("## 3. 数据模型", "数据模型章节缺失"),
    ("## 4. 接口设计", "接口设计章节缺失"),
]

NAMING_PATTERN = re.compile(
    r"^[A-Z]+_HLD_(PROJECT"
    r"|SERVICE_[A-Za-z][A-Za-z0-9-]*"
    r"|FEATURE_[A-Za-z][A-Za-z0-9-]*_[A-Za-z0-9-]+)"
    r"_v\d+\.\d+_\d{8}\.md$"
)


def check_naming(filepath):
    issues = []
    name = Path(filepath).name
    if not NAMING_PATTERN.match(name):
        issues.append(
            "文件名不符合规范:[项目缩写]_HLD_[级别]_[模块]_vX.Y_YYYYMMDD.md"
        )
    return issues


def check_header(content):
    issues = []
    for field in REQUIRED_HEADER_FIELDS:
        if field not in content:
            issues.append(f"头部缺字段:{field}")
    return issues


def check_revision_table(content):
    issues = []
    if "| 版本号 | 修订日期 | 修订人 | 修订内容 | 审核人 |" not in content:
        issues.append("修订记录表缺失")
    return issues


def check_sections(content):
    issues = []
    for marker, err in REQUIRED_SECTIONS:
        if marker not in content:
            issues.append(err)
    return issues


def check_mermaid(content):
    issues = []
    if "```mermaid" not in content:
        issues.append("处理流程图必须用 mermaid 语法(缺少 ```mermaid 代码块)")
    return issues


def main():
    if len(sys.argv) < 2:
        print(
            "Usage: check_hld_structure.py <file1.md> [file2.md ...]",
            file=sys.stderr,
        )
        sys.exit(2)

    all_issues = []
    for filepath in sys.argv[1:]:
        if not os.path.exists(filepath):
            all_issues.append(f"{filepath}: 文件不存在")
            continue
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        file_issues = []
        file_issues += check_naming(filepath)
        file_issues += check_header(content)
        file_issues += check_revision_table(content)
        file_issues += check_sections(content)
        file_issues += check_mermaid(content)

        if file_issues:
            all_issues.append(f"\n{filepath}:")
            for i in file_issues:
                all_issues.append(f"  - {i}")

    if all_issues:
        print("\n".join(all_issues), file=sys.stderr)
        sys.exit(1)
    else:
        print("✓ 所有 HLD 文件校验通过")
        sys.exit(0)


if __name__ == "__main__":
    main()
