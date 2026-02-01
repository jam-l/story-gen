package com.novelsim.app.domain.rpg

import com.novelsim.app.data.model.CharacterStats
import com.novelsim.app.data.model.Enemy
import com.novelsim.app.data.model.EnemyDrop
import com.novelsim.app.data.model.Skill
import kotlin.random.Random

/**
 * 战斗系统 - 回合制战斗逻辑
 */
class BattleSystem {
    
    /**
     * 战斗阶段
     */
    enum class BattlePhase {
        PLAYER_TURN,    // 玩家回合
        ENEMY_TURN,     // 敌人回合
        VICTORY,        // 胜利
        DEFEAT          // 失败
    }
    
    /**
     * 战斗动作
     */
    sealed class BattleAction {
        object Attack : BattleAction()
        data class Skill(val skillId: String) : BattleAction()
        data class UseItem(val itemId: String) : BattleAction()
        object Defend : BattleAction()
        object Flee : BattleAction()
    }
    
    /**
     * 战斗日志
     */
    data class BattleLog(
        val message: String,
        val type: LogType = LogType.INFO
    ) {
        enum class LogType {
            INFO, DAMAGE, HEAL, BUFF, CRITICAL
        }
    }
    
    /**
     * 战斗状态
     */
    data class BattleState(
        val playerStats: CharacterStats,
        val enemy: Enemy,
        val enemyCurrentHp: Int,
        val turn: Int = 1,
        val phase: BattlePhase = BattlePhase.PLAYER_TURN,
        val logs: List<BattleLog> = emptyList(),
        val isDefending: Boolean = false
    ) {
        val isFinished: Boolean
            get() = phase == BattlePhase.VICTORY || phase == BattlePhase.DEFEAT
    }
    
    /**
     * 战斗奖励
     */
    data class BattleReward(
        val exp: Int,
        val gold: Int,
        val drops: List<Pair<String, Int>>
    )
    
    /**
     * 开始战斗
     */
    fun startBattle(playerStats: CharacterStats, enemy: Enemy): BattleState {
        return BattleState(
            playerStats = playerStats,
            enemy = enemy,
            enemyCurrentHp = enemy.stats.currentHp,
            logs = listOf(BattleLog("${enemy.name} 出现了！", BattleLog.LogType.INFO))
        )
    }
    
    /**
     * 执行玩家回合
     */
    fun executePlayerTurn(state: BattleState, action: BattleAction, skills: Map<String, Skill>): BattleState {
        if (state.phase != BattlePhase.PLAYER_TURN) return state
        
        var newState = state.copy(isDefending = false)
        val logs = mutableListOf<BattleLog>()
        
        when (action) {
            is BattleAction.Attack -> {
                val (damage, isCritical) = calculateDamage(
                    attackerAtk = state.playerStats.attack,
                    defenderDef = state.enemy.stats.defense,
                    attackerLuck = state.playerStats.luck
                )
                val newEnemyHp = (state.enemyCurrentHp - damage).coerceAtLeast(0)
                
                logs.add(
                    BattleLog(
                        if (isCritical) "暴击！对 ${state.enemy.name} 造成 $damage 点伤害！"
                        else "对 ${state.enemy.name} 造成 $damage 点伤害",
                        if (isCritical) BattleLog.LogType.CRITICAL else BattleLog.LogType.DAMAGE
                    )
                )
                
                newState = newState.copy(enemyCurrentHp = newEnemyHp)
                
                if (newEnemyHp <= 0) {
                    logs.add(BattleLog("${state.enemy.name} 被击败了！", BattleLog.LogType.INFO))
                    newState = newState.copy(phase = BattlePhase.VICTORY)
                }
            }
            
            is BattleAction.Defend -> {
                logs.add(BattleLog("你进入了防御姿态", BattleLog.LogType.BUFF))
                newState = newState.copy(isDefending = true)
            }
            
            is BattleAction.Flee -> {
                val fleeChance = 0.3 + (state.playerStats.speed - state.enemy.stats.speed) * 0.05
                if (Random.nextFloat() < fleeChance) {
                    logs.add(BattleLog("逃跑成功！", BattleLog.LogType.INFO))
                    newState = newState.copy(phase = BattlePhase.DEFEAT) // 逃跑视为失败
                } else {
                    logs.add(BattleLog("逃跑失败！", BattleLog.LogType.INFO))
                }
            }
            
            is BattleAction.Skill -> {
                val skill = skills[action.skillId]
                if (skill != null) {
                    if (state.playerStats.currentMp >= skill.mpCost) {
                        val newMp = state.playerStats.currentMp - skill.mpCost
                        var currentEnemyHp = state.enemyCurrentHp
                        var newHp = state.playerStats.currentHp
                        
                        // Damage
                        if (skill.damage > 0) {
                             val totalAtk = state.playerStats.attack + skill.damage
                             val (damage, isCritical) = calculateDamage(totalAtk, state.enemy.stats.defense, state.playerStats.luck)
                             currentEnemyHp = (currentEnemyHp - damage).coerceAtLeast(0)
                             logs.add(BattleLog("使用了 ${skill.name}，对 ${state.enemy.name} 造成 $damage 点伤害！", if(isCritical) BattleLog.LogType.CRITICAL else BattleLog.LogType.DAMAGE))
                        }
                        
                        // Heal
                        if (skill.heal > 0) {
                             newHp = (newHp + skill.heal).coerceAtMost(state.playerStats.maxHp)
                             logs.add(BattleLog("使用了 ${skill.name}，恢复了 ${skill.heal} 点生命！", BattleLog.LogType.HEAL))
                        }
                        
                        if (skill.damage == 0 && skill.heal == 0) {
                             logs.add(BattleLog("使用了 ${skill.name}！", BattleLog.LogType.INFO))
                        }
                        
                        newState = newState.copy(
                            playerStats = newState.playerStats.copy(currentHp = newHp, currentMp = newMp),
                            enemyCurrentHp = currentEnemyHp
                        )
                        
                        if (currentEnemyHp <= 0) {
                            logs.add(BattleLog("${state.enemy.name} 被击败了！", BattleLog.LogType.INFO))
                            newState = newState.copy(phase = BattlePhase.VICTORY)
                        }
                    } else {
                        logs.add(BattleLog("MP不足！", BattleLog.LogType.INFO))
                    }
                } else {
                    logs.add(BattleLog("技能不存在！", BattleLog.LogType.INFO))
                }
            }
            
            is BattleAction.UseItem -> {
                // TODO: 实现道具使用
                logs.add(BattleLog("道具使用开发中...", BattleLog.LogType.INFO))
            }
        }
        
        // 如果战斗未结束且不是逃跑，进入敌人回合
        if (!newState.isFinished && action !is BattleAction.Flee) {
            newState = newState.copy(phase = BattlePhase.ENEMY_TURN)
        }
        
        return newState.copy(logs = state.logs + logs)
    }
    
    /**
     * 执行敌人回合
     */
    fun executeEnemyTurn(state: BattleState): BattleState {
        if (state.phase != BattlePhase.ENEMY_TURN) return state
        
        val logs = mutableListOf<BattleLog>()
        
        // 敌人攻击
        val (baseDamage, isCritical) = calculateDamage(
            attackerAtk = state.enemy.stats.attack,
            defenderDef = state.playerStats.defense,
            attackerLuck = state.enemy.stats.luck
        )
        
        // 防御减伤
        val damage = if (state.isDefending) baseDamage / 2 else baseDamage
        val newPlayerHp = (state.playerStats.currentHp - damage).coerceAtLeast(0)
        
        logs.add(
            BattleLog(
                if (state.isDefending) "${state.enemy.name} 攻击了你，但你成功防御，受到 $damage 点伤害"
                else if (isCritical) "暴击！${state.enemy.name} 对你造成 $damage 点伤害！"
                else "${state.enemy.name} 对你造成 $damage 点伤害",
                if (isCritical) BattleLog.LogType.CRITICAL else BattleLog.LogType.DAMAGE
            )
        )
        
        val newPlayerStats = state.playerStats.copy(currentHp = newPlayerHp)
        var newPhase = BattlePhase.PLAYER_TURN
        var newTurn = state.turn
        
        if (newPlayerHp <= 0) {
            logs.add(BattleLog("你被击败了...", BattleLog.LogType.INFO))
            newPhase = BattlePhase.DEFEAT
        } else {
            newTurn = state.turn + 1
        }
        
        return state.copy(
            playerStats = newPlayerStats,
            phase = newPhase,
            turn = newTurn,
            logs = state.logs + logs,
            isDefending = false
        )
    }
    
    /**
     * 计算伤害
     */
    private fun calculateDamage(
        attackerAtk: Int,
        defenderDef: Int,
        attackerLuck: Int
    ): Pair<Int, Boolean> {
        // 基础伤害 = 攻击力 * 2 - 防御力，最少1点
        val baseDamage = (attackerAtk * 2 - defenderDef).coerceAtLeast(1)
        
        // 伤害浮动 ±10%
        val variance = Random.nextFloat() * 0.2f - 0.1f
        var damage = (baseDamage * (1 + variance)).toInt().coerceAtLeast(1)
        
        // 暴击判定（基于幸运值）
        val critChance = attackerLuck * 0.01f
        val isCritical = Random.nextFloat() < critChance
        if (isCritical) {
            damage = (damage * 1.5f).toInt()
        }
        
        return Pair(damage, isCritical)
    }
    
    /**
     * 计算战斗奖励
     */
    fun calculateReward(state: BattleState): BattleReward? {
        if (state.phase != BattlePhase.VICTORY) return null
        
        val drops = state.enemy.drops.mapNotNull { drop ->
            if (Random.nextFloat() < drop.chance) {
                val quantity = Random.nextInt(drop.minQuantity, drop.maxQuantity + 1)
                drop.itemId to quantity
            } else null
        }
        
        return BattleReward(
            exp = state.enemy.expReward,
            gold = state.enemy.goldReward,
            drops = drops
        )
    }
}
