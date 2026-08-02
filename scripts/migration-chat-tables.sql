-- Migration: bảng chat hỗ trợ (Conversations, Messages)
-- Chạy trên database Sports4Life khi ddl-auto=none

IF OBJECT_ID(N'dbo.Messages', N'U') IS NOT NULL
    DROP TABLE [dbo].[Messages];
GO
IF OBJECT_ID(N'dbo.Conversations', N'U') IS NOT NULL
    DROP TABLE [dbo].[Conversations];
GO

CREATE TABLE [dbo].[Conversations](
    [Id] [bigint] IDENTITY(1,1) NOT NULL,
    [UserId] [int] NOT NULL,
    [EmployeeId] [int] NULL,
    [Status] [varchar](20) NOT NULL CONSTRAINT [DF_Conversations_Status] DEFAULT ('OPEN'),
    [CreatedAt] [datetime] NOT NULL CONSTRAINT [DF_Conversations_CreatedAt] DEFAULT (getdate()),
    [UpdatedAt] [datetime] NULL,
    CONSTRAINT [PK_Conversations] PRIMARY KEY CLUSTERED ([Id] ASC)
) ON [PRIMARY];
GO

CREATE NONCLUSTERED INDEX [IX_Conversations_User_Status]
    ON [dbo].[Conversations] ([UserId] ASC, [Status] ASC);
GO

CREATE NONCLUSTERED INDEX [IX_Conversations_Employee_Status]
    ON [dbo].[Conversations] ([EmployeeId] ASC, [Status] ASC);
GO

CREATE TABLE [dbo].[Messages](
    [Id] [bigint] IDENTITY(1,1) NOT NULL,
    [ConversationId] [bigint] NOT NULL,
    [SenderType] [varchar](20) NOT NULL,
    [SenderId] [bigint] NULL,
    [MessageType] [varchar](20) NOT NULL CONSTRAINT [DF_Messages_MessageType] DEFAULT ('TEXT'),
    [Content] [nvarchar](4000) NOT NULL,
    [CreatedAt] [datetime] NOT NULL CONSTRAINT [DF_Messages_CreatedAt] DEFAULT (getdate()),
    [Seen] [bit] NOT NULL CONSTRAINT [DF_Messages_Seen] DEFAULT ((0)),
    CONSTRAINT [PK_Messages] PRIMARY KEY CLUSTERED ([Id] ASC)
) ON [PRIMARY];
GO

CREATE NONCLUSTERED INDEX [IX_Messages_Conversation_CreatedAt]
    ON [dbo].[Messages] ([ConversationId] ASC, [CreatedAt] ASC);
GO

ALTER TABLE [dbo].[Conversations] WITH CHECK ADD CONSTRAINT [FK_Conversations_Users]
    FOREIGN KEY([UserId]) REFERENCES [dbo].[Users] ([AccountId]);
GO
ALTER TABLE [dbo].[Conversations] CHECK CONSTRAINT [FK_Conversations_Users];
GO

ALTER TABLE [dbo].[Conversations] WITH CHECK ADD CONSTRAINT [FK_Conversations_Employees]
    FOREIGN KEY([EmployeeId]) REFERENCES [dbo].[Employees] ([AccountId]);
GO
ALTER TABLE [dbo].[Conversations] CHECK CONSTRAINT [FK_Conversations_Employees];
GO

ALTER TABLE [dbo].[Messages] WITH CHECK ADD CONSTRAINT [FK_Messages_Conversations]
    FOREIGN KEY([ConversationId]) REFERENCES [dbo].[Conversations] ([Id]);
GO
ALTER TABLE [dbo].[Messages] CHECK CONSTRAINT [FK_Messages_Conversations];
GO
