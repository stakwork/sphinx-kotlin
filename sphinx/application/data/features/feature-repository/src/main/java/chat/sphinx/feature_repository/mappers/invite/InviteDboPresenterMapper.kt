package chat.sphinx.feature_repository.mappers.invite

import chat.sphinx.conceptcoredb.InviteDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_invite.Invite
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class InviteDboPresenterMapper(dispatchers: CoroutineDispatchers): ClassMapper<InviteDbo, Invite>(dispatchers) {
    override suspend fun mapFrom(value: InviteDbo): Invite {
        return Invite(
            value.id,
            value.invite_string,
            null,
            value.invoice,
            value.contact_id,
            value.status,
            value.price,
            value.created_at,
        )
    }

    override suspend fun mapTo(value: Invite): InviteDbo {
        return InviteDbo(
            value.id,
            value.inviteString,
            value.inviteCode,
            value.paymentRequest,
            value.contactId,
            value.status,
            value.price,
            value.createdAt,
        )
    }
}
