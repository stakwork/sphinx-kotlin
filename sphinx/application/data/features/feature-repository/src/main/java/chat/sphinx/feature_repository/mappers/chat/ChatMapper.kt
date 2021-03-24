package chat.sphinx.feature_repository.mappers.chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.featurecoredb.ChatDbo
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_common.toSeen

internal class ChatMapper: ClassMapper<ChatDto, ChatDbo, Chat>() {

    override fun fromDTOtoDBO(dto: ChatDto): ChatDbo {
        return ChatDbo(
            ChatId(dto.id),
            ChatUUID(dto.uuid),
            dto.name?.toChatName(),
            dto.photo_url?.toPhotoUrl(),
            dto.type.toChatType(),
            dto.status.toChatStatus(),
            dto.contact_ids.map { ContactId(it) },
            dto.is_muted.toChatMuted(),
            DateTime(dto.created_at),
            dto.group_key?.toChatGroupKey(),
            dto.host?.toChatHost(),
            dto.price_per_message?.toSat(),
            dto.escrow_amount?.toSat(),
            dto.unlisted.toChatUnlisted(),
            dto.private.toChatPrivate(),
            dto.owner_pub_key?.toLightningNodePubKey(),
            dto.seen.toSeen(),
            dto.meta?.toChatMetaDataOrNull(),
            dto.my_photo_url?.toPhotoUrl(),
            dto.my_alias?.toChatAlias(),
            dto.pending_contact_ids?.map { ContactId(it) }
        )
    }

    override fun fromDTOtoPresenter(dto: ChatDto): Chat {
        return Chat(
            ChatId(dto.id),
            ChatUUID(dto.uuid),
            dto.name?.toChatName(),
            dto.photo_url?.toPhotoUrl(),
            dto.type.toChatType(),
            dto.status.toChatStatus(),
            dto.contact_ids.map { ContactId(it) },
            dto.is_muted.toChatMuted(),
            DateTime(dto.created_at),
            dto.group_key?.toChatGroupKey(),
            dto.host?.toChatHost(),
            dto.price_per_message?.toSat(),
            dto.escrow_amount?.toSat(),
            dto.unlisted.toChatUnlisted(),
            dto.private.toChatPrivate(),
            dto.owner_pub_key?.toLightningNodePubKey(),
            dto.seen.toSeen(),
            dto.meta?.toChatMetaDataOrNull(),
            dto.my_photo_url?.toPhotoUrl(),
            dto.my_alias?.toChatAlias(),
            dto.pending_contact_ids?.map { ContactId(it) }
        )
    }

    override fun fromDTOsToDBOs(dtos: List<ChatDto>): List<ChatDbo> {
        return dtos.map { fromDTOtoDBO(it) }
    }

    override fun fromDTOsToPresenters(dtos: List<ChatDto>): List<Chat> {
        return dtos.map { fromDTOtoPresenter(it) }
    }

    override fun fromDBOtoPresenter(dbo: ChatDbo): Chat {
        return Chat(
            dbo.id,
            dbo.uuid,
            dbo.name,
            dbo.photo_url,
            dbo.type,
            dbo.status,
            dbo.contact_ids,
            dbo.is_muted,
            dbo.created_at,
            dbo.group_key,
            dbo.host,
            dbo.price_per_message,
            dbo.escrow_amount,
            dbo.unlisted,
            dbo.private_tribe,
            dbo.owner_pub_key,
            dbo.seen,
            dbo.meta_data,
            dbo.my_photo_url,
            dbo.my_alias,
            dbo.pending_contact_ids
        )
    }

    override fun fromDBOsToPresenters(dbos: List<ChatDbo>): List<Chat> {
        return dbos.map { fromDBOtoPresenter(it) }
    }

    override fun fromPresenterToDBO(presenter: Chat): ChatDbo {
        return ChatDbo(
            presenter.id,
            presenter.uuid,
            presenter.name,
            presenter.photoUrl,
            presenter.type,
            presenter.status,
            presenter.contactIds,
            presenter.isMuted,
            presenter.createdAt,
            presenter.groupKey,
            presenter.host,
            presenter.pricePerMessage,
            presenter.escrowAmount,
            presenter.unlisted,
            presenter.privateTribe,
            presenter.ownerPubKey,
            presenter.seen,
            presenter.metaData,
            presenter.myPhotoUrl,
            presenter.myAlias,
            presenter.pendingContactIds,
        )
    }

    override fun fromPresentersToDBOs(presenters: List<Chat>): List<ChatDbo> {
        return presenters.map { fromPresenterToDBO(it) }
    }
}