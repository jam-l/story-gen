import random

# 定义故事节点：每个节点是一个字典，包含描述、选项和下一个节点
story = {
    "start": {
        "description": "你醒来在一个神秘的森林中。四周雾气缭绕，你看到两条小路：左边通往河流，右边通往山洞。你选择？",
        "options": {
            "1": {"text": "去河流", "next": "river"},
            "2": {"text": "去山洞", "next": "cave"}
        }
    },
    "river": {
        "description": "你来到河流边，看到一条小船。你可以划船过河或沿河岸走。",
        "options": {
            "1": {"text": "划船过河", "next": "cross_river"},
            "2": {"text": "沿河岸走", "next": "shore"}
        }
    },
    "cave": {
        "description": "山洞里漆黑一片，你点亮火把，发现宝箱和怪物。你选择？",
        "options": {
            "1": {"text": "打开宝箱", "next": "treasure"},
            "2": {"text": "对抗怪物", "next": "monster"}
        }
    },
    "cross_river": {
        "description": "你成功过河，找到一座城堡。故事结束：你成为英雄！",
        "options": {}  # 空字典表示结束
    },
    "shore": {
        "description": "沿河岸走，你遇到野兽。随机事件：",
        "options": {}  # 将在代码中处理随机
    },
    "treasure": {
        "description": "宝箱里有金币，但触发陷阱。你逃脱了，但受伤。故事结束。",
        "options": {}
    },
    "monster": {
        "description": "你勇敢对抗怪物，但失败了。游戏结束。",
        "options": {}
    }
}

def play_game():
    current_node = "start"
    while True:
        node = story[current_node]
        print(node["description"])
        
        if not node["options"]:  # 结束节点
            if current_node == "shore":
                # 添加随机元素
                outcomes = ["你击败野兽，找到宝藏！", "野兽攻击你，游戏结束。"]
                print(random.choice(outcomes))
            break
        
        # 显示选项
        for key, option in node["options"].items():
            print(f"{key}: {option['text']}")
        
        # 获取用户输入
        choice = input("输入你的选择 (1/2 等): ")
        if choice in node["options"]:
            current_node = node["options"][choice]["next"]
        else:
            print("无效选择，请重试。")

if __name__ == "__main__":
    print("欢迎来到小说模拟器！")
    play_game()