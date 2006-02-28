<?xml version="1.0" encoding="UTF-8"?>
<!-- (c) Copyright IBM Corp. 2004, 2005 All Rights Reserved. -->

<!--  generalize.xsl 
 | Convert specialied DITA topics into revertable, "generalized" form
 *-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

    <xsl:output method="xml" indent="no" 
	doctype-public="-//MEKON AND BLAST RADIUS//DTD Framemaker-compatible DITA//EN"
	doctype-system="fm_dita.dtd"/>
    <xsl:preserve-space elements="*"/>

<!-- Configuration: Tables use external or internal titles?
     Possible values: internal (titles are FrameMaker table titles; desc is lost)
                      external (titles and desc are adjacent paragraphs outside table)
     -->
<xsl:param name="config-table-title">external</xsl:param>

<!-- Configuration: Create FrameMaker book processing instructions?
     Possible values: yes (document will ask to be made into a book on loading into FrameMaker)
                      no (monolithic XML document)
     -->
<xsl:param name="config-book">no</xsl:param>

<!-- Configuration: If creating a book, how to break into chapters?
     Possible values: one (one giant book for the entire content)
                      type (group bookmap types (preface, chapter, appendix) into one book each)
                      each (every top-level topic is its own chapter)
     -->
<xsl:param name="config-chapter-grouping">one</xsl:param>

<xsl:variable name="apos">'</xsl:variable>


    <!-- delete these attributes -->
    <xsl:template match="@DTDVersion"/>
    <xsl:template match="@refclass"/>
    <xsl:template match="@navtitle"/>

    <xsl:template match="related-links" priority="5"/>

    <!-- Match text: Strip whitespace where it ought to vanish. -->
    <xsl:template match="text()">
      <xsl:variable name="is-block">
        <xsl:apply-templates select="." mode="is-block"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$is-block = ''"/>     <!-- Text vanishes. -->
        <xsl:otherwise>
          <!-- Replace straight apostrophes with curved ones. -->
          <xsl:variable name="t">
            <xsl:call-template name="translate-string"/>
          </xsl:variable>
          <!-- Adapted (heavily) from http://skew.org/xml/stylesheets/trim/trim.xsl -->
  	  <xsl:variable name="first-char" select="substring($t, 1, 1)"/>
          <xsl:variable name="last-char" select="substring($t, string-length($t))"/>
	  <xsl:variable name="had-leading-ws" select="not(normalize-space($first-char))"/>
	  <xsl:variable name="had-trailing-ws" select="not(normalize-space($last-char))"/>
          <xsl:variable name="preceding-block">
            <xsl:apply-templates select="preceding-sibling::node()[1]" mode="is-block-chase-preceding"/>
          </xsl:variable>
          <xsl:variable name="following-block">
            <xsl:apply-templates select="following-sibling::node()[1]" mode="is-block-chase-following"/>
          </xsl:variable>
          <!-- Output leading space iff it was present in the original and preceding sibling is inline. -->
	  <xsl:if test="$preceding-block = 'n' and $had-leading-ws"><xsl:text> </xsl:text></xsl:if>
	  <xsl:value-of select="normalize-space($t)"/>
          <!-- Output trailing space iff it was present in the original and following sibling is inline. -->
	  <xsl:if test="$following-block and $had-trailing-ws and normalize-space($t)"><xsl:text> </xsl:text></xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>

 
    <!-- Ignore these elements (more prolog elements TODO) -->
    <xsl:template match="*[contains(@class, ' map/topicref ') and not(contains(@class, ' mapgroup-d/topichead '))] |
                         *[contains(@class, ' topic/prolog ')] |
                         *[contains(@class, ' topic/keywords ')] |
                         *[contains(@class, ' topic/metadata ')] " priority="5">
	<xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="/">
	<xsl:processing-instruction name="FM"> Validation Off</xsl:processing-instruction>
        <xsl:if test="$config-book = 'yes'">
	  <xsl:processing-instruction name="FM"> book </xsl:processing-instruction>
        </xsl:if>
       <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="@*">
      <xsl:copy/>
    </xsl:template>

    <xsl:template match="@class|@refclass" mode="simplify-class">
                <xsl:variable name="clsString" select="normalize-space(substring-after(.,' '))"></xsl:variable>
                <xsl:choose>
                    <xsl:when test="substring-after($clsString,' ')=''">
                        <xsl:value-of select="translate($clsString,'/','.')"/>
                    </xsl:when>
                    <xsl:otherwise>
                           <xsl:value-of select="translate(substring-after($clsString,' '),'/','.')"/>
                    </xsl:otherwise>
                </xsl:choose>
    </xsl:template>

    <xsl:template match="@class">
      <xsl:attribute name="class">
        <xsl:apply-templates select="." mode="simplify-class"/>
      </xsl:attribute>
    </xsl:template>

    <xsl:template match="@refclass">
      <xsl:attribute name="refclass">
        <xsl:apply-templates select="." mode="simplify-class"/>
      </xsl:attribute>
    </xsl:template>

    <xsl:template match="@id">
	<xsl:processing-instruction name="Fm">MARKER [Hypertext] newlink <xsl:value-of select="."/></xsl:processing-instruction>
    </xsl:template>

    <xsl:template name="wrap-fm-pi-begin">
        <xsl:if test="@audience">
            <xsl:processing-instruction name="Fm">condstart <xsl:value-of select="@audience"
            /></xsl:processing-instruction>
        </xsl:if>
        <xsl:if test="@product">
            <xsl:processing-instruction name="Fm">condstart <xsl:value-of select="@product"
            /></xsl:processing-instruction>
        </xsl:if>
    </xsl:template>

    <xsl:template name="wrap-fm-pi-end">
        <xsl:if test="@product">
            <xsl:processing-instruction name="Fm">condend <xsl:value-of select="@product"
            /></xsl:processing-instruction>
        </xsl:if>
        <xsl:if test="@audience">
            <xsl:processing-instruction name="Fm">condend <xsl:value-of select="@audience"
            /></xsl:processing-instruction>
        </xsl:if>
    </xsl:template>

    <xsl:template match="*[@class]">
          <xsl:apply-templates select="." mode="replace-tag"/>
    </xsl:template>

  <xsl:template match="*[@class]" mode="replace-tag">
          <xsl:call-template name="wrap-fm-pi-begin"/>
              <xsl:variable name="generalize" select="substring-before(substring-after(@class,'/'),' ')"/>
              <xsl:element name="{$generalize}">
                  <xsl:apply-templates select="@*[name() != 'id']"/>
                  <xsl:apply-templates select="@id"/>
                  <xsl:apply-templates select="." mode="replace-content"/>
              </xsl:element>
        <xsl:call-template name="wrap-fm-pi-end"/>
  </xsl:template>

   <xsl:template match="*" mode="replace-content">
     <xsl:apply-templates select="." mode="process-children"/>
   </xsl:template>

   <!-- Process child nodes for the current node, ensuring that block elements and inline elements don't mix. -->
   <xsl:template match="*" mode="process-children">
      <xsl:variable name="is-block">
        <xsl:apply-templates select="." mode="is-block"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$is-block = 'n'">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="child-block">
            <xsl:apply-templates select="node()" mode="is-block"/>
          </xsl:variable>
          <xsl:choose>
            <xsl:when test="contains($child-block, 'y') and contains($child-block, 'n')">
              <xsl:for-each select="child::node()">
                <xsl:variable name="is-block">
                  <xsl:apply-templates select="." mode="is-block"/>
                </xsl:variable>
                <xsl:variable name="preceding-block">
                  <xsl:apply-templates select="preceding-sibling::node()[1]" mode="is-block-chase-preceding"/>
                </xsl:variable>
                <xsl:choose>
                  <!-- Block elements need no attention. -->
                  <xsl:when test="$is-block = 'y'">
                    <xsl:apply-templates select="."/>
                  </xsl:when>
                  <xsl:when test="$preceding-block = 'n'"/> <!-- Skip -->
                  <!-- Start of inline sequence, needs wrapper. -->
                  <xsl:when test="$is-block = 'n'">
                    <fm_div>
                      <xsl:apply-templates select="." mode="walk-inline-wrap"/>
                    </fm_div>
                  </xsl:when>
                  <!-- Otherwise is vanished node outside block. -->
                  <xsl:otherwise>
                    <xsl:apply-templates select="."/>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template match="node()" mode="walk-inline-wrap">
     <xsl:variable name="is-block">
       <xsl:apply-templates select="." mode="is-block"/>
     </xsl:variable>
     <xsl:choose>
       <xsl:when test="$is-block = 'y'"/>
       <xsl:otherwise>
         <xsl:apply-templates select="."/>
         <xsl:apply-templates select="following-sibling::node()[1]" mode="walk-inline-wrap"/>
       </xsl:otherwise>
     </xsl:choose>
   </xsl:template>

   <!-- Handle special node types -->

   <xsl:template match="comment()|processing-instruction()">
          <xsl:copy>
            <xsl:apply-templates/>
	</xsl:copy>
    </xsl:template>

<!-- Index markers require escaping of \ : ; [ ] &lt; &gt; with backslashes (\ needs an extra level of escaping). -->
<xsl:template name="escape-pi-metacharacters">
        <xsl:param name="s"/>
        <xsl:variable name="t" select="translate($s, '\[]:;&lt;&gt;', ':::::')"/>
        <xsl:choose>
          <xsl:when test="not(contains($t, ':'))">
            <xsl:value-of select="$s"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="substring-before($t, ':')"/>
            <xsl:text>\\</xsl:text>
            <xsl:value-of select="substring($s, 1 + string-length(substring-before($t, ':')), 1)"/>
            <xsl:if test="substring($s, 1 + string-length(substring-before($t, ':')), 1) = '\'">
              <xsl:text>\</xsl:text>
            </xsl:if>
            <xsl:call-template name="escape-pi-metacharacters">
                    <xsl:with-param name="s" select="substring($s, 2 + string-length(substring-before($t, ':')))"/>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
</xsl:template>

<xsl:template name="translate-string">
  <xsl:param name="s" select="."/>
  <xsl:value-of select="translate(string($s), $apos, '&#x2019;')"/>
</xsl:template>

<xsl:template name="translate-string-in-pi">
  <xsl:param name="s" select="."/>
  <xsl:variable name="t">
    <xsl:call-template name="translate-string">
      <xsl:with-param name="s" select="$s"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="escape-pi-metacharacters">
    <xsl:with-param name="s" select="translate($t, '&#x09;&#x0A;&#x0d;', '&#x20;&#x20;&#x20;')"/>
  </xsl:call-template>
</xsl:template>

<xsl:include href="dltable.xsl"/>
<xsl:include href="image.xsl"/>
<xsl:include href="indexterm.xsl"/>
<xsl:include href="inline-block.xsl"/>
<xsl:include href="map.xsl"/>
<xsl:include href="pre.xsl"/>
<xsl:include href="simpletable.xsl"/>
<xsl:include href="table.xsl"/>
<xsl:include href="tm.xsl"/>
<xsl:include href="xref.xsl"/>
<xsl:include href="topic.xsl"/>

</xsl:stylesheet>