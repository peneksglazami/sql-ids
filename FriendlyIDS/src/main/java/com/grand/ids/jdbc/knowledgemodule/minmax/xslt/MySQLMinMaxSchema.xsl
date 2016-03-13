<?xml version="1.0" encoding="UTF-8"?>
<!--
   Copyright 2009-2016 Andrey Grigorov

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="text"/>
    <xsl:template match="/">
        <xsl:for-each select="//Table">
            <xsl:call-template name="CloneTable">
                <xsl:with-param name="tableNode" select="current()"/>
                <xsl:with-param name="newName" select="concat(current()/@name,  '_min')"/>
            </xsl:call-template>
            <xsl:call-template name="CloneTable">
                <xsl:with-param name="tableNode" select="current()"/>
                <xsl:with-param name="newName" select="concat(current()/@name,  '_max')"/>
            </xsl:call-template>
            <xsl:call-template name="CountTable">
                <xsl:with-param name="tableNode" select="current()"/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:call-template name="GeneralTable">
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="CloneTable">
        <xsl:param name="tableNode"/>
        <xsl:param name="newName"/>
        <xsl:text>CREATE TABLE </xsl:text>
        <xsl:value-of select="$newName"/>
        <xsl:text> (IDS_USER_ID varchar(255) NOT NULL, </xsl:text>
        <xsl:for-each select="$tableNode/Columns/Column">
            <xsl:if test="position() != 1">
                <xsl:text>,</xsl:text>
            </xsl:if>
            <xsl:text>&#xA;</xsl:text>
            <xsl:text>  </xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="@typeName"/>
            <xsl:if test="@typeName = 'varchar' or @typeName = 'varbinary'">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="@size"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="count($tableNode/PrimaryKeys/PrimaryKey) > 0">
            <xsl:text>,</xsl:text>
            <xsl:text>&#xA;</xsl:text>
            <xsl:text>  PRIMARY KEY (IDS_USER_ID, </xsl:text>
            <xsl:for-each select="$tableNode/PrimaryKeys/PrimaryKey">
                <xsl:if test="position() != 1">
                    <xsl:text>, </xsl:text>
                </xsl:if>
                <xsl:value-of select="current()"/>
            </xsl:for-each>
            <xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>);</xsl:text>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>&#xA;</xsl:text>
    </xsl:template>
    <xsl:template name="CountTable">
        <xsl:param name="tableNode"/>
        <xsl:text>CREATE TABLE </xsl:text>
        <xsl:value-of select="$tableNode/@name"/>
        <xsl:text>_cnt (IDS_USER_ID varchar(255) NOT NULL, </xsl:text>
        <xsl:for-each select="$tableNode/PrimaryKeys/PrimaryKey">
            <xsl:text>&#xA;</xsl:text>
            <xsl:text>  </xsl:text>
            <xsl:value-of select="current()"/>
            <xsl:text> </xsl:text>
            <xsl:variable name="column" select="$tableNode/Columns/Column[@name = current()]"/>
            <xsl:value-of select="$column/@typeName"/>
            <xsl:if test="$column/@typeName = 'varchar' or $column/@typeName = 'varbinary'">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="$column/@size"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
            <xsl:text>,</xsl:text>
        </xsl:for-each>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>  HITS bigint unsigned,</xsl:text>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>  START_TIME bigint unsigned,</xsl:text>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>  PRIMARY KEY (IDS_USER_ID, </xsl:text>
        <xsl:for-each select="$tableNode/PrimaryKeys/PrimaryKey">
            <xsl:if test="position() != 1">
                <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:value-of select="current()"/>
        </xsl:for-each>
        <xsl:text>)</xsl:text>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>);</xsl:text>
        <xsl:text>&#xA;</xsl:text>
        <xsl:text>&#xA;</xsl:text>
    </xsl:template>
    <xsl:template name="GeneralTable">
        <xsl:text>CREATE TABLE GENERAL_TABLE (TABLE_NAME varchar(255) NOT NULL, IDS_USER_ID varchar(255) NOT NULL, TABLE_TIME bigint unsigned, PRIMARY KEY (TABLE_NAME, IDS_USER_ID));</xsl:text>
    </xsl:template>
</xsl:stylesheet>
