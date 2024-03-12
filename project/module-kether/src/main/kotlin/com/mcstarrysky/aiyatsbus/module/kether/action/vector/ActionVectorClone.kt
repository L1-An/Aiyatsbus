package com.mcstarrysky.aiyatsbus.module.kether.action.vector

/**
 * Vulpecula
 * com.mcstarrysky.aiyatsbus.module.kether.action.vector
 *
 * @author Lanscarlos
 * @since 2023-03-22 15:01
 */
object ActionVectorClone : ActionVector.Resolver {

    override val name: Array<String> = arrayOf("clone")

    /**
     * vec clone &vec
     * */
    override fun resolve(reader: ActionVector.Reader): ActionVector.Handler<out Any?> {
        return reader.transfer {
            combine(
                source()
            ) { vector ->
                vector.clone()
            }
        }
    }
}