-- Migration: tạo bảng Conversations + Messages cho Floating Chat Widget
-- Chạy trên SQL Server (database hiện tại của dự án)
-- User đã đăng nhập = Users (không dùng GuestId)

IF OBJECT_ID(N'dbo.Conversations', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Conversations] (
        [Id]         BIGINT IDENTITY(1,1) NOT NULL,
        [UserId]     BIGINT               NOT NULL,
        [EmployeeId] BIGINT               NULL,
        [Status]     NVARCHAR(20)         NOT NULL CONSTRAINT [DF_Conversations_Status] DEFAULT (N'OPEN'),
        [CreatedAt]  DATETIME2            NOT NULL CONSTRAINT [DF_Conversations_CreatedAt] DEFAULT (SYSUTCDATETIME()),
        [UpdatedAt]  DATETIME2            NULL,
        CONSTRAINT [PK_Conversations] PRIMARY KEY CLUSTERED ([Id]),
        CONSTRAINT [FK_Conversations_User] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Users] ([AccountId]),
        CONSTRAINT [FK_Conversations_Employee] FOREIGN KEY ([EmployeeId]) REFERENCES [dbo].[Employees] ([AccountId]),
        CONSTRAINT [CK_Conversations_Status] CHECK ([Status] IN (N'OPEN', N'CLOSED', N'RESOLVED'))
    );

    CREATE INDEX [IX_Conversations_User_Status]
        ON [dbo].[Conversations] ([UserId], [Status]);

    CREATE INDEX [IX_Conversations_Employee_Status]
        ON [dbo].[Conversations] ([EmployeeId], [Status]);
END
GO

IF OBJECT_ID(N'dbo.Messages', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Messages] (
        [Id]             BIGINT IDENTITY(1,1) NOT NULL,
        [ConversationId] BIGINT               NOT NULL,
        [SenderType]     NVARCHAR(20)         NOT NULL,
        [SenderId]       BIGINT               NULL,
        [MessageType]    NVARCHAR(20)         NOT NULL CONSTRAINT [DF_Messages_MessageType] DEFAULT (N'TEXT'),
        [Content]        NVARCHAR(4000)       NOT NULL,
        [CreatedAt]      DATETIME2            NOT NULL CONSTRAINT [DF_Messages_CreatedAt] DEFAULT (SYSUTCDATETIME()),
        [Seen]           BIT                  NOT NULL CONSTRAINT [DF_Messages_Seen] DEFAULT ((0)),
        CONSTRAINT [PK_Messages] PRIMARY KEY CLUSTERED ([Id]),
        CONSTRAINT [FK_Messages_Conversation] FOREIGN KEY ([ConversationId]) REFERENCES [dbo].[Conversations] ([Id]),
        CONSTRAINT [CK_Messages_SenderType] CHECK ([SenderType] IN (N'USER', N'EMPLOYEE', N'SYSTEM')),
        CONSTRAINT [CK_Messages_MessageType] CHECK ([MessageType] IN (N'TEXT', N'IMAGE', N'FILE', N'SYSTEM'))
    );

    CREATE INDEX [IX_Messages_Conversation_CreatedAt]
        ON [dbo].[Messages] ([ConversationId], [CreatedAt]);
END
GO
